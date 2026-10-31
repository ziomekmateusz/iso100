package catalog

import (
	"cmp"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"slices"
	"strings"
)

type outputCamera struct {
	ID      string   `json:"id"`
	Maker   string   `json:"maker"`
	Model   string   `json:"model"`
	Format  string   `json:"format"`
	Mount   string   `json:"mount,omitempty"`
	Aliases []string `json:"aliases,omitempty"`
	Specs   *Specs   `json:"specs,omitempty"`
}

type output struct {
	SchemaVersion int            `json:"schemaVersion"`
	Version       string         `json:"version"`
	Attribution   string         `json:"attribution,omitempty"`
	Formats       []Format       `json:"formats"`
	Cameras       []outputCamera `json:"cameras"`
}

// Build validates the catalog and returns the content of catalog.json. The output is
// deterministic: version is the first 12 hex digits of the SHA-256 of the output with an empty version.
func (c *Catalog) Build() ([]byte, error) {
	if errors, _ := Count(c.Validate()); errors > 0 {
		return nil, fmt.Errorf("the catalog has %d errors, run validate", errors)
	}
	out := output{SchemaVersion: SchemaVersion, Formats: c.Formats, Cameras: []outputCamera{}}
	for _, m := range c.Makers {
		for _, cam := range m.Cameras {
			out.Cameras = append(out.Cameras, outputCamera{
				ID: cam.ID, Maker: m.Maker, Model: cam.Model, Format: cam.Format,
				Mount: cam.Mount, Aliases: cam.Aliases, Specs: cam.Specs,
			})
			if cam.Source == SourceLensfun {
				out.Attribution = Attribution
			}
		}
	}
	slices.SortFunc(out.Cameras, func(a, b outputCamera) int {
		return cmp.Or(compareFold(a.Maker, b.Maker), compareFold(a.Model, b.Model), strings.Compare(a.ID, b.ID))
	})
	unversioned, err := Encode(out)
	if err != nil {
		return nil, err
	}
	sum := sha256.Sum256(unversioned)
	out.Version = hex.EncodeToString(sum[:])[:12]
	return Encode(out)
}
