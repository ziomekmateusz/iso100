package catalog

import (
	"fmt"
	"strings"
	"unicode"
)

type Severity string

const (
	Error   Severity = "error"
	Warning Severity = "warning"
)

type Issue struct {
	Severity Severity
	File     string
	ID       string
	Message  string
}

func (i Issue) String() string {
	return fmt.Sprintf("%s: %s: %s: %s", i.Severity, i.File, i.ID, i.Message)
}

// Count returns the number of errors and warnings.
func Count(issues []Issue) (errors, warnings int) {
	for _, i := range issues {
		if i.Severity == Error {
			errors++
		} else {
			warnings++
		}
	}
	return errors, warnings
}

// Validate checks formats and cameras: valid and unique ids, known formats, no repeated models or
// aliases, file names that match the maker, plausible specs and clean text.
func (c *Catalog) Validate() []Issue {
	var issues []Issue
	report := func(sev Severity, file, id, format string, args ...any) {
		issues = append(issues, Issue{sev, file, id, fmt.Sprintf(format, args...)})
	}
	text := func(file, id, field, s string) {
		if p := textProblem(s); p != "" {
			report(Error, file, id, "%s %q: %s", field, s, p)
		}
	}

	formats := map[string]bool{}
	for i, f := range c.Formats {
		id := label(f.ID, i)
		switch {
		case !validID(f.ID):
			report(Error, FormatsFile, id, "invalid id")
		case formats[f.ID]:
			report(Error, FormatsFile, id, "duplicate id")
		}
		formats[f.ID] = true
		if f.Name == "" {
			report(Error, FormatsFile, id, "empty name")
		}
		text(FormatsFile, id, "name", f.Name)
		if !inRange(f.WidthMm) || !inRange(f.HeightMm) {
			report(Error, FormatsFile, id, "dimensions must be in (0, 100] mm")
		}
		if f.WidthMm < f.HeightMm {
			report(Error, FormatsFile, id, "width smaller than height")
		}
	}

	ids := map[string]string{}
	makers := map[string]bool{}
	for _, m := range c.Makers {
		file := CamerasDir + "/" + m.File
		switch key := strings.ToLower(m.Maker); {
		case m.Maker == "":
			report(Error, file, "-", "empty maker")
		case makers[key]:
			report(Error, file, "-", "maker %q repeated", m.Maker)
		case m.File != Slug(m.Maker)+".json":
			report(Error, file, "-", "file must be named %s.json", Slug(m.Maker))
		}
		makers[strings.ToLower(m.Maker)] = true
		text(file, "-", "maker", m.Maker)
		if len(m.Cameras) == 0 {
			report(Warning, file, "-", "maker without cameras")
		}

		models := map[string]string{}  // model → id
		aliases := map[string]string{} // alias → id
		for i, cam := range m.Cameras {
			id := label(cam.ID, i)
			switch {
			case !validID(cam.ID):
				report(Error, file, id, "invalid id")
			case ids[cam.ID] != "":
				report(Error, file, id, "duplicate id, also in %s", ids[cam.ID])
			default:
				ids[cam.ID] = file
			}
			if m.Maker != "" && !strings.HasPrefix(cam.ID, Slug(m.Maker)+"-") {
				report(Warning, file, id, "id does not start with %s-", Slug(m.Maker))
			}
			if cam.Model == "" {
				report(Error, file, id, "empty model")
			} else if other, ok := models[strings.ToLower(cam.Model)]; ok {
				report(Error, file, id, "model %q repeated, also in %s", cam.Model, other)
			} else {
				models[strings.ToLower(cam.Model)] = id
			}
			text(file, id, "model", cam.Model)
			text(file, id, "mount", cam.Mount)
			if !formats[cam.Format] {
				report(Error, file, id, "unknown format %q", cam.Format)
			}
			if cam.Source != SourceCurated && cam.Source != SourceLensfun {
				report(Error, file, id, "source must be %q or %q", SourceCurated, SourceLensfun)
			}
			for _, a := range cam.Aliases {
				key := strings.ToLower(a)
				switch {
				case a == "":
					report(Error, file, id, "empty alias")
				case strings.EqualFold(a, cam.Model):
					report(Error, file, id, "alias %q equal to the model", a)
				case aliases[key] != "":
					report(Error, file, id, "alias %q repeated, also in %s", a, aliases[key])
				default:
					aliases[key] = id
				}
				text(file, id, "alias", a)
			}
			for _, p := range specsProblems(cam.Specs) {
				report(Error, file, id, "specs: %s", p)
			}
		}
		for i, cam := range m.Cameras {
			for _, a := range cam.Aliases {
				if other, ok := models[strings.ToLower(a)]; ok && other != label(cam.ID, i) {
					report(Warning, file, label(cam.ID, i), "alias %q is the model of %s", a, other)
				}
			}
		}
	}
	return issues
}

func label(id string, index int) string {
	if id == "" {
		return fmt.Sprintf("#%d", index+1)
	}
	return id
}

func inRange(mm float64) bool { return mm > 0 && mm <= 100 }

func textProblem(s string) string {
	switch {
	case strings.ContainsFunc(s, unicode.IsControl):
		return "control characters"
	case s != strings.TrimSpace(s):
		return "leading or trailing spaces"
	case strings.Contains(s, "  "):
		return "double spaces"
	}
	return ""
}

func specsProblems(s *Specs) []string {
	if s == nil {
		return nil
	}
	var p []string
	negative := func(name string, v float64) {
		if v < 0 {
			p = append(p, name+" negative")
		}
	}
	if s.IsoMin != nil {
		negative("isoMin", float64(*s.IsoMin))
	}
	if s.IsoMax != nil {
		negative("isoMax", float64(*s.IsoMax))
	}
	if s.IsoMin != nil && s.IsoMax != nil && *s.IsoMin >= *s.IsoMax {
		p = append(p, "isoMin >= isoMax")
	}
	if s.ResolutionMp != nil {
		negative("resolutionMp", *s.ResolutionMp)
		if *s.ResolutionMp > 500 {
			p = append(p, "resolutionMp > 500")
		}
	}
	if s.AfPoints != nil {
		negative("afPoints", float64(*s.AfPoints))
	}
	if s.Fps != nil {
		negative("fps", *s.Fps)
		if *s.Fps > 120 {
			p = append(p, "fps > 120")
		}
	}
	if s.WeightG != nil {
		negative("weightG", float64(*s.WeightG))
		if *s.WeightG > 10000 {
			p = append(p, "weightG > 10000")
		}
	}
	return p
}
