package catalog

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func ptr[T any](v T) *T { return &v }

func validCatalog() *Catalog {
	return &Catalog{
		Formats: []Format{
			{ID: "full-frame", Name: "Full Frame", WidthMm: 36, HeightMm: 24},
			{ID: "aps-c", Name: "APS-C", WidthMm: 23.5, HeightMm: 15.6},
		},
		Makers: []*Maker{{Maker: "Sony", File: "sony.json", Cameras: []Camera{
			{ID: "sony-alpha-7-iii", Model: "Alpha 7 III", Format: "full-frame", Mount: "Sony E", Aliases: []string{"ILCE-7M3"},
				Source: SourceCurated, Specs: &Specs{IsoMin: ptr(50), IsoMax: ptr(204800), ResolutionMp: ptr(24.2)}},
			{ID: "sony-alpha-6400", Model: "Alpha 6400", Format: "aps-c", Source: SourceLensfun},
		}}},
	}
}

func TestSlug(t *testing.T) {
	tests := []struct{ in, want string }{
		{"Sony Alpha 7 III", "sony-alpha-7-iii"},
		{"Canon EOS 5D Mark IV", "canon-eos-5d-mark-iv"},
		{"Leica S (Typ 007)", "leica-s-typ-007"},
		{"Fujifilm X-T3", "fujifilm-x-t3"},
		{"APS-H 27×18", "aps-h-27x18"},
		{"Olympus µ-mini", "olympus-u-mini"},
		{"Zażółć gęślą jaźń", "zazolc-gesla-jazn"},
		{"Città più ÉCLAIR", "citta-piu-eclair"},
		{"  --Hasselblad 500 mech.--  ", "hasselblad-500-mech"},
		{"Зенит", ""},
	}
	for _, tt := range tests {
		if got := Slug(tt.in); got != tt.want {
			t.Errorf("Slug(%q) = %q, want %q", tt.in, got, tt.want)
		}
	}
}

func TestValidate(t *testing.T) {
	if issues := validCatalog().Validate(); len(issues) > 0 {
		t.Fatalf("valid catalog has issues: %v", issues)
	}
	first := func(c *Catalog) *Camera { return &c.Makers[0].Cameras[0] }
	second := func(c *Catalog) *Camera { return &c.Makers[0].Cameras[1] }
	tests := []struct {
		name   string
		mutate func(c *Catalog)
		sev    Severity
		msg    string
	}{
		{"format invalid id", func(c *Catalog) { c.Formats[0].ID = "Full Frame" }, Error, "invalid id"},
		{"format duplicate id", func(c *Catalog) { c.Formats[1].ID = "full-frame" }, Error, "duplicate id"},
		{"format empty name", func(c *Catalog) { c.Formats[0].Name = "" }, Error, "empty name"},
		{"format zero height", func(c *Catalog) { c.Formats[0].HeightMm = 0 }, Error, "(0, 100]"},
		{"format too wide", func(c *Catalog) { c.Formats[0].WidthMm = 101 }, Error, "(0, 100]"},
		{"format portrait", func(c *Catalog) { c.Formats[0].WidthMm = 20 }, Error, "width smaller than height"},
		{"empty maker", func(c *Catalog) { c.Makers[0].Maker = "" }, Error, "empty maker"},
		{"file name", func(c *Catalog) { c.Makers[0].File = "Sony.json" }, Error, "must be named sony.json"},
		{"repeated maker", func(c *Catalog) {
			c.Makers = append(c.Makers, &Maker{Maker: "SONY", File: "sony.json", Cameras: []Camera{
				{ID: "sony-nex-5", Model: "NEX-5", Format: "aps-c", Source: SourceLensfun}}})
		}, Error, "repeated"},
		{"maker without cameras", func(c *Catalog) { c.Makers[0].Cameras = nil }, Warning, "without cameras"},
		{"invalid camera id", func(c *Catalog) { first(c).ID = "Sony A7 III" }, Error, "invalid id"},
		{"duplicate camera id", func(c *Catalog) { second(c).ID = first(c).ID }, Error, "duplicate id"},
		{"id without maker prefix", func(c *Catalog) { first(c).ID = "alpha-7-iii" }, Warning, "does not start with sony-"},
		{"empty model", func(c *Catalog) { first(c).Model = "" }, Error, "empty model"},
		{"repeated model", func(c *Catalog) { second(c).Model = "ALPHA 7 III" }, Error, "repeated"},
		{"unknown format", func(c *Catalog) { first(c).Format = "fromat" }, Error, `unknown format "fromat"`},
		{"bad source", func(c *Catalog) { first(c).Source = "manual" }, Error, "source must be"},
		{"empty alias", func(c *Catalog) { first(c).Aliases = []string{""} }, Error, "empty alias"},
		{"alias equal to model", func(c *Catalog) { first(c).Aliases = []string{"alpha 7 iii"} }, Error, "equal to the model"},
		{"alias repeated", func(c *Catalog) { second(c).Aliases = []string{"ilce-7m3"} }, Error, "repeated"},
		{"alias is another model", func(c *Catalog) { first(c).Aliases = []string{"Alpha 6400"} }, Warning, "is the model of sony-alpha-6400"},
		{"negative specs", func(c *Catalog) { first(c).Specs.IsoMin = ptr(-1) }, Error, "isoMin negative"},
		{"iso range", func(c *Catalog) { first(c).Specs.IsoMin = ptr(204800) }, Error, "isoMin >= isoMax"},
		{"resolution", func(c *Catalog) { first(c).Specs.ResolutionMp = ptr(600.0) }, Error, "resolutionMp > 500"},
		{"fps", func(c *Catalog) { first(c).Specs.Fps = ptr(121.0) }, Error, "fps > 120"},
		{"weight", func(c *Catalog) { first(c).Specs.WeightG = ptr(10001) }, Error, "weightG > 10000"},
		{"trailing space", func(c *Catalog) { first(c).Model = "D3500 " }, Error, "leading or trailing spaces"},
		{"control character", func(c *Catalog) { c.Makers[0].Maker = "Sony\n" }, Error, "control characters"},
		{"double space", func(c *Catalog) { first(c).Mount = "Sony  E" }, Error, "double spaces"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := validCatalog()
			tt.mutate(c)
			issues := c.Validate()
			for _, i := range issues {
				if i.Severity == tt.sev && strings.Contains(i.Message, tt.msg) {
					return
				}
			}
			t.Errorf("want %s containing %q, got %v", tt.sev, tt.msg, issues)
		})
	}
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

const formatsJSON = `{"formats": [{"id": "full-frame", "name": "Full Frame", "widthMm": 36, "heightMm": 24}]}`

func TestLoadIsStrict(t *testing.T) {
	tests := []struct{ name, sony, wantErr string }{
		{"valid", `{"maker": "Sony", "cameras": [{"id": "sony-a", "model": "A", "format": "full-frame", "source": "iso100"}]}`, ""},
		{"unknown field", `{"maker": "Sony", "cameras": [{"id": "sony-a", "model": "A", "fromat": "full-frame", "source": "iso100"}]}`, `unknown field "fromat"`},
		{"content after the value", `{"maker": "Sony", "cameras": []} {}`, "content after the top-level value"},
		{"malformed", `{"maker": "Sony",`, "unexpected EOF"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			dir := t.TempDir()
			writeFile(t, filepath.Join(dir, FormatsFile), formatsJSON)
			writeFile(t, filepath.Join(dir, CamerasDir, "sony.json"), tt.sony)
			c, err := Load(dir)
			if tt.wantErr == "" {
				if err != nil || len(c.Makers) != 1 || c.Makers[0].File != "sony.json" {
					t.Fatalf("Load = %+v, %v", c, err)
				}
				return
			}
			if err == nil || !strings.Contains(err.Error(), tt.wantErr) {
				t.Fatalf("want error containing %q, got %v", tt.wantErr, err)
			}
		})
	}
}

func TestWriteMakersIsCanonical(t *testing.T) {
	dir := t.TempDir()
	writeFile(t, filepath.Join(dir, FormatsFile), formatsJSON)
	c := validCatalog()
	changed, err := c.WriteMakers(dir)
	if err != nil || len(changed) != 1 {
		t.Fatalf("WriteMakers = %v, %v", changed, err)
	}
	written, _ := os.ReadFile(filepath.Join(dir, CamerasDir, "sony.json"))
	if !bytes.HasSuffix(written, []byte("}\n")) || !bytes.Contains(written, []byte("\n  \"cameras\": [\n")) {
		t.Errorf("not indented with 2 spaces and a final newline:\n%s", written)
	}
	if a, b := bytes.Index(written, []byte("Alpha 6400")), bytes.Index(written, []byte("Alpha 7 III")); a > b {
		t.Errorf("cameras not sorted by model:\n%s", written)
	}
	reloaded, err := Load(dir)
	if err != nil {
		t.Fatal(err)
	}
	again, err := reloaded.Makers[0].Canonical()
	if err != nil || !bytes.Equal(again, written) {
		t.Errorf("reading and writing again changes the file:\n%s", again)
	}
	if changed, err := reloaded.WriteMakers(dir); err != nil || len(changed) != 0 {
		t.Errorf("second WriteMakers = %v, %v, want nothing changed", changed, err)
	}
}

func TestBuild(t *testing.T) {
	c := validCatalog()
	out, err := c.Build()
	if err != nil {
		t.Fatal(err)
	}
	again, _ := c.Build()
	if !bytes.Equal(out, again) {
		t.Fatal("two builds give different bytes")
	}

	var got output
	if err := json.Unmarshal(out, &got); err != nil {
		t.Fatal(err)
	}
	version := got.Version
	got.Version = ""
	unversioned, _ := Encode(got)
	sum := sha256.Sum256(unversioned)
	if want := hex.EncodeToString(sum[:])[:12]; version != want {
		t.Errorf("version = %q, want %q", version, want)
	}
	if got.SchemaVersion != 1 || got.Attribution != Attribution {
		t.Errorf("schemaVersion %d, attribution %q", got.SchemaVersion, got.Attribution)
	}
	if bytes.Contains(out, []byte(`"source"`)) {
		t.Error("source must not be in the output")
	}
	if got.Cameras[0].ID != "sony-alpha-6400" || got.Cameras[0].Maker != "Sony" {
		t.Errorf("cameras not sorted by maker and model: %+v", got.Cameras)
	}

	c.Makers[0].Cameras[1].Source = SourceCurated
	curated, _ := c.Build()
	if bytes.Contains(curated, []byte(`"attribution"`)) {
		t.Error("attribution without Lensfun cameras")
	}

	c.Makers[0].Cameras[1].Format = "fromat"
	if _, err := c.Build(); err == nil {
		t.Error("Build must fail when the catalog has errors")
	}
}
