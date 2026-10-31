// Package catalog reads, validates, formats and builds the camera catalog in data/.
package catalog

import (
	"bytes"
	"cmp"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"math"
	"os"
	"path/filepath"
	"slices"
	"strings"
)

const (
	FormatsFile = "formats.json"
	CamerasDir  = "cameras"

	SourceCurated   = "iso100"
	SourceLensfun   = "lensfun"
	SchemaVersion   = 1
	Attribution     = "Camera data partly derived from the Lensfun database (https://lensfun.github.io), licensed under CC BY-SA 3.0."
	fullFrameWidth  = 36.0
	fullFrameHeight = 24.0
)

type Format struct {
	ID       string  `json:"id"`
	Name     string  `json:"name"`
	WidthMm  float64 `json:"widthMm"`
	HeightMm float64 `json:"heightMm"`
}

// CropFactor is the ratio between the full frame diagonal and the format diagonal.
func (f Format) CropFactor() float64 {
	return math.Hypot(fullFrameWidth, fullFrameHeight) / math.Hypot(f.WidthMm, f.HeightMm)
}

type Specs struct {
	IsoMin        *int     `json:"isoMin,omitempty"`
	IsoMax        *int     `json:"isoMax,omitempty"`
	ResolutionMp  *float64 `json:"resolutionMp,omitempty"`
	AfPoints      *int     `json:"afPoints,omitempty"`
	Fps           *float64 `json:"fps,omitempty"`
	Stabilization *bool    `json:"stabilization,omitempty"`
	WeatherSealed *bool    `json:"weatherSealed,omitempty"`
	Wifi          *bool    `json:"wifi,omitempty"`
	WeightG       *int     `json:"weightG,omitempty"`
}

type Camera struct {
	ID      string   `json:"id"`
	Model   string   `json:"model"`
	Format  string   `json:"format"`
	Mount   string   `json:"mount,omitempty"`
	Aliases []string `json:"aliases,omitempty"`
	Source  string   `json:"source"`
	Specs   *Specs   `json:"specs,omitempty"`
}

// Maker is one data/cameras/<slug(maker)>.json file.
type Maker struct {
	Maker   string   `json:"maker"`
	Cameras []Camera `json:"cameras"`
	File    string   `json:"-"` // file name inside CamerasDir
}

type Catalog struct {
	Formats []Format
	Makers  []*Maker
}

// Load reads dir strictly: unknown fields and content after the top-level value are errors.
func Load(dir string) (*Catalog, error) {
	var formats struct {
		Formats []Format `json:"formats"`
	}
	if err := decodeFile(filepath.Join(dir, FormatsFile), &formats); err != nil {
		return nil, err
	}
	c := &Catalog{Formats: formats.Formats}
	entries, err := os.ReadDir(filepath.Join(dir, CamerasDir))
	if err != nil && !errors.Is(err, fs.ErrNotExist) {
		return nil, err
	}
	for _, e := range entries {
		if e.IsDir() || filepath.Ext(e.Name()) != ".json" {
			continue
		}
		m := &Maker{File: e.Name()}
		if err := decodeFile(filepath.Join(dir, CamerasDir, e.Name()), m); err != nil {
			return nil, err
		}
		c.Makers = append(c.Makers, m)
	}
	return c, nil
}

func decodeFile(path string, v any) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	dec := json.NewDecoder(bytes.NewReader(data))
	dec.DisallowUnknownFields()
	if err := dec.Decode(v); err != nil {
		return fmt.Errorf("%s: %w", filepath.ToSlash(path), err)
	}
	if _, err := dec.Token(); err != io.EOF {
		return fmt.Errorf("%s: content after the top-level value", filepath.ToSlash(path))
	}
	return nil
}

// Encode returns v as JSON indented by 2 spaces, with a final newline and no HTML escaping.
func Encode(v any) ([]byte, error) {
	var b bytes.Buffer
	enc := json.NewEncoder(&b)
	enc.SetEscapeHTML(false)
	enc.SetIndent("", "  ")
	if err := enc.Encode(v); err != nil {
		return nil, err
	}
	return b.Bytes(), nil
}

// Canonical returns the canonical content of the maker file: cameras sorted by model, then id.
func (m *Maker) Canonical() ([]byte, error) {
	c := *m
	c.Cameras = slices.Clone(m.Cameras)
	slices.SortStableFunc(c.Cameras, func(a, b Camera) int {
		return cmp.Or(compareFold(a.Model, b.Model), strings.Compare(a.ID, b.ID))
	})
	return Encode(c)
}

// FindMaker returns the maker with the given name, ignoring case.
func (c *Catalog) FindMaker(name string) *Maker {
	for _, m := range c.Makers {
		if strings.EqualFold(m.Maker, name) {
			return m
		}
	}
	return nil
}

// WriteMakers writes every maker file in canonical form and returns the files whose content changed.
func (c *Catalog) WriteMakers(dir string) ([]string, error) {
	if err := os.MkdirAll(filepath.Join(dir, CamerasDir), 0o755); err != nil {
		return nil, err
	}
	var changed []string
	for _, m := range c.Makers {
		data, err := m.Canonical()
		if err != nil {
			return changed, err
		}
		path := filepath.Join(dir, CamerasDir, m.File)
		if old, err := os.ReadFile(path); err == nil && bytes.Equal(old, data) {
			continue
		}
		if err := os.WriteFile(path, data, 0o644); err != nil {
			return changed, err
		}
		changed = append(changed, CamerasDir+"/"+m.File)
	}
	return changed, nil
}

func compareFold(a, b string) int {
	return strings.Compare(strings.ToLower(a), strings.ToLower(b))
}
