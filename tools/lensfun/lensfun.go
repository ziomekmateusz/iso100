// Package lensfun reads the cameras of the Lensfun database and merges them into the catalog.
package lensfun

import (
	"cmp"
	"encoding/xml"
	"fmt"
	"math"
	"os"
	"path/filepath"
	"slices"
	"strconv"
	"strings"

	"github.com/ziomekmateusz/iso100/tools/catalog"
)

// DefaultGroups are the interchangeable lens groups (file name prefix before the first "-").
var DefaultGroups = []string{"slr", "mil", "rf", "om"}

// Camera is a cleaned <camera> element.
type Camera struct {
	Maker      string
	Model      string
	Aliases    []string // the EXIF name, when it differs from the model
	Mount      string
	CropFactor float64
	File       string
}

type xmlName struct {
	Lang  string `xml:"lang,attr"`
	Value string `xml:",chardata"`
}

type xmlCamera struct {
	Makers     []xmlName `xml:"maker"`
	Models     []xmlName `xml:"model"`
	Variant    string    `xml:"variant"`
	Mount      string    `xml:"mount"`
	CropFactor string    `xml:"cropfactor"`
}

// Read returns the cameras of the given groups found in the Lensfun db directory, and how many
// were skipped (maker Generic, "film:" pseudo-cameras, missing crop factor).
func Read(dir string, groups []string) (cams []Camera, skipped int, err error) {
	files, err := filepath.Glob(filepath.Join(dir, "*.xml"))
	if err != nil {
		return nil, 0, err
	}
	if len(files) == 0 {
		return nil, 0, fmt.Errorf("no .xml files in %s", dir)
	}
	slices.Sort(files)
	for _, file := range files {
		name := filepath.Base(file)
		group, _, _ := strings.Cut(strings.TrimSuffix(name, ".xml"), "-")
		if !slices.Contains(groups, group) {
			continue
		}
		data, err := os.ReadFile(file)
		if err != nil {
			return nil, 0, err
		}
		var db struct {
			Cameras []xmlCamera `xml:"camera"`
		}
		if err := xml.Unmarshal(data, &db); err != nil {
			return nil, 0, fmt.Errorf("%s: %w", name, err)
		}
		for _, x := range db.Cameras {
			if c, ok := clean(x, name); ok {
				cams = append(cams, c)
			} else {
				skipped++
			}
		}
	}
	return cams, skipped, nil
}

func clean(x xmlCamera, file string) (Camera, bool) {
	makerEn, makerExif := pick(x.Makers)
	modelEn, modelExif := pick(x.Models)
	maker := cmp.Or(makerEn, makerExif)
	cf, err := strconv.ParseFloat(strings.TrimSpace(x.CropFactor), 64)
	if maker == "" || strings.EqualFold(maker, "Generic") || err != nil || cf <= 0 ||
		strings.Contains(strings.ToLower(modelEn+" "+modelExif), "film:") {
		return Camera{}, false
	}
	exif := trimModel(modelExif, maker, makerExif)
	model := cmp.Or(trimModel(modelEn, maker, makerExif), exif)
	if model == "" {
		return Camera{}, false
	}
	c := Camera{Maker: maker, Model: model, Mount: strings.TrimSpace(x.Mount), CropFactor: cf, File: file}
	if exif != "" && !strings.EqualFold(exif, model) {
		c.Aliases = []string{exif}
	}
	if v := strings.TrimSpace(x.Variant); v != "" {
		c.Model += " (" + v + ")"
	}
	return c, true
}

// pick returns the English name and the name without lang attribute (the EXIF one).
func pick(names []xmlName) (en, exif string) {
	for _, n := range names {
		switch n.Lang {
		case "en":
			en = strings.Join(strings.Fields(n.Value), " ")
		case "":
			exif = strings.Join(strings.Fields(n.Value), " ")
		}
	}
	return en, exif
}

// trimModel removes the maker prefix ("Canon EOS 90D" → "EOS 90D") and the " Digital Camera" suffix.
func trimModel(model string, makers ...string) string {
	for _, m := range makers {
		if m != "" && len(model) > len(m) && strings.EqualFold(model[:len(m)+1], m+" ") {
			model = model[len(m)+1:]
			break
		}
	}
	return strings.TrimSuffix(model, " Digital Camera")
}

// MatchFormat returns the format with the closest crop factor, if the relative difference is within tolerance.
func MatchFormat(formats []catalog.Format, cropFactor, tolerance float64) (catalog.Format, bool) {
	best, bestDiff := catalog.Format{}, math.Inf(1)
	for _, f := range formats {
		if d := math.Abs(cropFactor-f.CropFactor()) / f.CropFactor(); d < bestDiff {
			best, bestDiff = f, d
		}
	}
	return best, bestDiff <= tolerance
}

type Report struct {
	Read, Unchanged                                  int
	Added, Updated, Unmatched, Duplicates, Conflicts []string
}

// Merge adds the Lensfun cameras to the catalog. It never deletes cameras nor changes ids:
// curated cameras only get an empty mount filled and missing aliases added, imported ones are
// updated, new ones are added. A format different from a curated camera's is a conflict.
func Merge(cat *catalog.Catalog, cams []Camera, tolerance float64) Report {
	r := Report{Read: len(cams)}
	seen := map[string]bool{}
	for _, lf := range cams {
		key := strings.ToLower(lf.Maker + "\x00" + lf.Model)
		if seen[key] {
			r.Duplicates = append(r.Duplicates, fmt.Sprintf("%s %s (%s)", lf.Maker, lf.Model, lf.File))
			continue
		}
		seen[key] = true
		format, ok := MatchFormat(cat.Formats, lf.CropFactor, tolerance)
		if !ok {
			r.Unmatched = append(r.Unmatched, fmt.Sprintf("%s %s: crop factor %g (%s)", lf.Maker, lf.Model, lf.CropFactor, lf.File))
			continue
		}
		maker := cat.FindMaker(lf.Maker)
		if maker == nil {
			maker = &catalog.Maker{Maker: lf.Maker, File: catalog.Slug(lf.Maker) + ".json"}
			cat.Makers = append(cat.Makers, maker)
		}
		id := catalog.Slug(lf.Maker + " " + lf.Model)
		cam := find(cat, maker, id, lf)
		if cam == nil {
			maker.Cameras = append(maker.Cameras, catalog.Camera{
				ID: id, Model: lf.Model, Format: format.ID, Mount: lf.Mount,
				Aliases: slices.Clone(lf.Aliases), Source: catalog.SourceLensfun,
			})
			r.Added = append(r.Added, id)
			continue
		}
		var changes []string
		if cam.Source == catalog.SourceCurated {
			if cam.Format != format.ID {
				r.Conflicts = append(r.Conflicts, fmt.Sprintf("%s: format %s, Lensfun says %s (crop factor %g)", cam.ID, cam.Format, format.ID, lf.CropFactor))
			}
			if cam.Mount == "" && lf.Mount != "" {
				cam.Mount = lf.Mount
				changes = append(changes, "mount "+lf.Mount)
			}
			for _, n := range append([]string{lf.Model}, lf.Aliases...) {
				if !hasName(*cam, n) {
					cam.Aliases = append(cam.Aliases, n)
					changes = append(changes, "alias "+n)
				}
			}
		} else {
			if cam.Format != format.ID {
				cam.Format = format.ID
				changes = append(changes, "format "+format.ID)
			}
			if cam.Mount != lf.Mount {
				cam.Mount = lf.Mount
				changes = append(changes, "mount "+lf.Mount)
			}
			var aliases []string
			for _, a := range lf.Aliases {
				if !strings.EqualFold(a, cam.Model) {
					aliases = append(aliases, a)
				}
			}
			if !slices.Equal(cam.Aliases, aliases) {
				cam.Aliases = aliases
				changes = append(changes, fmt.Sprintf("aliases %q", aliases))
			}
		}
		if len(changes) == 0 {
			r.Unchanged++
		} else {
			r.Updated = append(r.Updated, cam.ID+": "+strings.Join(changes, "; "))
		}
	}
	return r
}

// find looks for an existing camera by id, then by maker and model, then by maker and alias.
func find(cat *catalog.Catalog, maker *catalog.Maker, id string, lf Camera) *catalog.Camera {
	for _, m := range cat.Makers {
		for i := range m.Cameras {
			if m.Cameras[i].ID == id {
				return &m.Cameras[i]
			}
		}
	}
	for i := range maker.Cameras {
		if strings.EqualFold(maker.Cameras[i].Model, lf.Model) {
			return &maker.Cameras[i]
		}
	}
	for i := range maker.Cameras {
		for _, n := range append([]string{lf.Model}, lf.Aliases...) {
			if hasName(maker.Cameras[i], n) {
				return &maker.Cameras[i]
			}
		}
	}
	return nil
}

func hasName(c catalog.Camera, name string) bool {
	return strings.EqualFold(c.Model, name) || slices.ContainsFunc(c.Aliases, func(a string) bool {
		return strings.EqualFold(a, name)
	})
}
