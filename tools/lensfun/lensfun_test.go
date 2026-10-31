package lensfun

import (
	"os"
	"path/filepath"
	"reflect"
	"slices"
	"testing"

	"github.com/ziomekmateusz/iso100/tools/catalog"
)

const milXML = `<?xml version="1.0" encoding="utf-8"?>
<lensdatabase version="2">
    <mount><name>Sony E</name></mount>
    <camera>
        <maker>Sony</maker>
        <model>ILCE-7M3</model>
        <model lang="en">Alpha 7 III</model>
        <mount>Sony E</mount>
        <cropfactor>1.0</cropfactor>
    </camera>
    <camera>
        <maker>Nikon Corporation</maker>
        <maker lang="en">Nikon</maker>
        <model>Nikon D850</model>
        <model lang="en">D850</model>
        <mount>Nikon F AF</mount>
        <cropfactor>1</cropfactor>
    </camera>
    <camera>
        <maker>Leica Camera AG</maker>
        <maker lang="en">Leica</maker>
        <model>M8 Digital Camera</model>
        <mount>Leica M</mount>
        <cropfactor>1.33</cropfactor>
    </camera>
    <camera>
        <maker>Kodak</maker>
        <model>DCS Pro 14N</model>
        <variant>DCS-14n</variant>
        <mount>Nikon F AF</mount>
        <cropfactor>1</cropfactor>
    </camera>
    <camera>
        <maker>Canon</maker>
        <model>35mm film: full frame</model>
        <mount>Canon EF</mount>
        <cropfactor>1</cropfactor>
    </camera>
    <camera>
        <maker>Generic</maker>
        <model>Crop-factor 1.5</model>
        <cropfactor>1.5</cropfactor>
    </camera>
    <camera>
        <maker>Canon</maker>
        <model>Canon EOS Without Crop</model>
        <mount>Canon EF</mount>
    </camera>
    <lens>
        <maker>Sony</maker>
        <model>FE 50mm f/1.8</model>
        <mount>Sony E</mount>
        <cropfactor>1</cropfactor>
    </lens>
</lensdatabase>
`

const omXML = `<lensdatabase version="2">
    <camera>
        <maker>OM Digital Solutions</maker>
        <maker lang="en">OM System</maker>
        <model>OM-1MarkII</model>
        <model lang="en">OM-1 II</model>
        <mount>Micro 4/3 System</mount>
        <cropfactor>2</cropfactor>
    </camera>
</lensdatabase>
`

const compactXML = `<lensdatabase version="2">
    <camera>
        <maker>Canon</maker>
        <model>Canon PowerShot G7 X</model>
        <mount>canonG7X</mount>
        <cropfactor>2.727</cropfactor>
    </camera>
</lensdatabase>
`

func TestRead(t *testing.T) {
	dir := t.TempDir()
	for name, content := range map[string]string{"mil-test.xml": milXML, "om-system.xml": omXML, "compact-canon.xml": compactXML} {
		if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	cams, skipped, err := Read(dir, DefaultGroups)
	if err != nil {
		t.Fatal(err)
	}
	want := []Camera{
		{Maker: "Sony", Model: "Alpha 7 III", Aliases: []string{"ILCE-7M3"}, Mount: "Sony E", CropFactor: 1, File: "mil-test.xml"},
		{Maker: "Nikon", Model: "D850", Mount: "Nikon F AF", CropFactor: 1, File: "mil-test.xml"},
		{Maker: "Leica", Model: "M8", Mount: "Leica M", CropFactor: 1.33, File: "mil-test.xml"},
		{Maker: "Kodak", Model: "DCS Pro 14N (DCS-14n)", Mount: "Nikon F AF", CropFactor: 1, File: "mil-test.xml"},
		{Maker: "OM System", Model: "OM-1 II", Aliases: []string{"OM-1MarkII"}, Mount: "Micro 4/3 System", CropFactor: 2, File: "om-system.xml"},
	}
	if !reflect.DeepEqual(cams, want) {
		t.Errorf("Read =\n%+v\nwant\n%+v", cams, want)
	}
	if skipped != 3 {
		t.Errorf("skipped = %d, want 3 (film, Generic, no crop factor)", skipped)
	}

	compact, _, err := Read(dir, []string{"compact"})
	if err != nil || len(compact) != 1 || compact[0].Model != "PowerShot G7 X" {
		t.Errorf("compact group = %+v, %v", compact, err)
	}
}

var testFormats = []catalog.Format{
	{ID: "full-frame", Name: "Full Frame", WidthMm: 36, HeightMm: 24},
	{ID: "aps-c", Name: "APS-C", WidthMm: 23.5, HeightMm: 15.6},
	{ID: "aps-c-canon", Name: "APS-C (Canon)", WidthMm: 22.3, HeightMm: 14.9},
	{ID: "four-thirds", Name: "Four Thirds", WidthMm: 17.3, HeightMm: 13},
}

func TestMatchFormat(t *testing.T) {
	tests := []struct {
		cropFactor float64
		want       string
		ok         bool
	}{
		{1, "full-frame", true},
		{1.534, "aps-c", true},
		{1.5, "aps-c", true},
		{1.613, "aps-c-canon", true},
		{1.587, "aps-c-canon", true},
		{2, "four-thirds", true},
		{1.2, "full-frame", false},
		{2.727, "four-thirds", false},
	}
	for _, tt := range tests {
		f, ok := MatchFormat(testFormats, tt.cropFactor, 0.03)
		if ok != tt.ok || (ok && f.ID != tt.want) {
			t.Errorf("MatchFormat(%g) = %s, %v; want %s, %v", tt.cropFactor, f.ID, ok, tt.want, tt.ok)
		}
	}
}

func testCatalog() *catalog.Catalog {
	return &catalog.Catalog{
		Formats: testFormats,
		Makers: []*catalog.Maker{
			{Maker: "Panasonic", File: "panasonic.json", Cameras: []catalog.Camera{
				{ID: "panasonic-lumix-g9", Model: "Lumix G9", Format: "four-thirds", Aliases: []string{"DC-G9"}, Source: catalog.SourceCurated},
				{ID: "panasonic-lumix-lx100-ii", Model: "Lumix LX100 II", Format: "four-thirds", Source: catalog.SourceCurated},
			}},
			{Maker: "Sony", File: "sony.json", Cameras: []catalog.Camera{
				{ID: "sony-alpha-7-iii", Model: "Alpha 7 III", Format: "aps-c", Source: catalog.SourceCurated},
				{ID: "sony-nex-5", Model: "NEX-5", Format: "full-frame", Mount: "old", Aliases: []string{"old"}, Source: catalog.SourceLensfun},
			}},
		},
	}
}

var lensfunCameras = []Camera{
	{Maker: "Panasonic", Model: "DC-G9", Mount: "Micro 4/3 System", CropFactor: 2},
	{Maker: "Sony", Model: "Alpha 7 III", Aliases: []string{"ILCE-7M3"}, Mount: "Sony E", CropFactor: 1},
	{Maker: "Sony", Model: "NEX-5", Mount: "Sony E", CropFactor: 1.534},
	{Maker: "sony", Model: "nex-5", Mount: "Sony E", CropFactor: 1.534},
	{Maker: "Pentax", Model: "K-1", Mount: "Pentax KAF2", CropFactor: 1},
	{Maker: "Sony", Model: "Odd", CropFactor: 1.2},
}

func TestMerge(t *testing.T) {
	cat := testCatalog()
	r := Merge(cat, lensfunCameras, 0.03)

	if r.Read != 6 || !slices.Equal(r.Added, []string{"pentax-k-1"}) || len(r.Duplicates) != 1 || len(r.Unmatched) != 1 {
		t.Errorf("report = %+v", r)
	}
	wantUpdated := []string{
		"panasonic-lumix-g9: mount Micro 4/3 System",
		"sony-alpha-7-iii: mount Sony E; alias ILCE-7M3",
		"sony-nex-5: format aps-c; mount Sony E; aliases []",
	}
	if !slices.Equal(r.Updated, wantUpdated) {
		t.Errorf("updated = %q, want %q", r.Updated, wantUpdated)
	}
	if len(r.Conflicts) != 1 || cat.Makers[1].Cameras[0].Format != "aps-c" {
		t.Errorf("a curated camera keeps its format and reports a conflict: %q", r.Conflicts)
	}
	if len(cat.Makers[0].Cameras) != 2 {
		t.Error("cameras missing from Lensfun must stay")
	}
	pentax := cat.FindMaker("Pentax")
	if pentax == nil || pentax.File != "pentax.json" || pentax.Cameras[0].Source != catalog.SourceLensfun || pentax.Cameras[0].Format != "full-frame" {
		t.Errorf("new maker = %+v", pentax)
	}
	if errs, warnings := catalog.Count(cat.Validate()); errs+warnings > 0 {
		t.Errorf("merged catalog has issues: %v", cat.Validate())
	}

	again := Merge(cat, lensfunCameras, 0.03)
	if len(again.Added) != 0 || len(again.Updated) != 0 || again.Unchanged != 4 {
		t.Errorf("second merge = %+v, want nothing added or updated", again)
	}
}
