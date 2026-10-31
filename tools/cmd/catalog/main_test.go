package main

import (
	"bytes"
	"os"
	"path/filepath"
	"testing"
)

const formatsJSON = `{"formats": [{"id": "full-frame", "name": "Full Frame", "widthMm": 36, "heightMm": 24}]}`

const sonyJSON = `{
  "maker": "Sony",
  "cameras": [
    {
      "id": "sony-alpha-7-iii",
      "model": "Alpha 7 III",
      "format": "full-frame",
      "source": "iso100"
    }
  ]
}
`

func dataDir(t *testing.T, sony string) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "cameras"), 0o755); err != nil {
		t.Fatal(err)
	}
	for path, content := range map[string]string{"formats.json": formatsJSON, "cameras/sony.json": sony} {
		if err := os.WriteFile(filepath.Join(dir, path), []byte(content), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	return dir
}

func exit(args ...string) int {
	var out bytes.Buffer
	return run(args, &out, &out)
}

func TestUsageErrors(t *testing.T) {
	tests := [][]string{
		nil,
		{"unknown"},
		{"validate", "-bogus"},
		{"build", "extra"},
		{"import-lensfun"},
	}
	for _, args := range tests {
		if got := exit(args...); got != 2 {
			t.Errorf("run(%q) = %d, want 2", args, got)
		}
	}
}

func TestValidate(t *testing.T) {
	if got := exit("validate", "-data", dataDir(t, sonyJSON)); got != 0 {
		t.Errorf("valid data: exit %d", got)
	}
	broken := bytes.Replace([]byte(sonyJSON), []byte(`"full-frame"`), []byte(`"fromat"`), 1)
	if got := exit("validate", "-data", dataDir(t, string(broken))); got != 1 {
		t.Errorf("unknown format: exit %d, want 1", got)
	}
}

func TestFmtCheck(t *testing.T) {
	dir := dataDir(t, `{"maker": "Sony", "cameras": [{"id": "sony-alpha-7-iii", "model": "Alpha 7 III", "format": "full-frame", "source": "iso100"}]}`)
	if got := exit("fmt", "-check", "-data", dir); got != 1 {
		t.Errorf("not canonical: exit %d, want 1", got)
	}
	if got := exit("fmt", "-data", dir); got != 0 {
		t.Fatalf("fmt: exit %d", got)
	}
	if got := exit("fmt", "-check", "-data", dir); got != 0 {
		t.Errorf("after fmt: exit %d, want 0", got)
	}
	if got, _ := os.ReadFile(filepath.Join(dir, "cameras", "sony.json")); string(got) != sonyJSON {
		t.Errorf("fmt wrote:\n%s", got)
	}
}

func TestBuildCheck(t *testing.T) {
	dir := dataDir(t, sonyJSON)
	out := filepath.Join(t.TempDir(), "assets", "catalog.json")
	if got := exit("build", "-check", "-data", dir, "-out", out); got != 1 {
		t.Errorf("missing output: exit %d, want 1", got)
	}
	if got := exit("build", "-data", dir, "-out", out); got != 0 {
		t.Fatalf("build: exit %d", got)
	}
	if got := exit("build", "-check", "-data", dir, "-out", out); got != 0 {
		t.Errorf("after build: exit %d, want 0", got)
	}
	changed := bytes.Replace([]byte(sonyJSON), []byte("Alpha 7 III"), []byte("Alpha 7 III (renamed)"), 1)
	if err := os.WriteFile(filepath.Join(dir, "cameras", "sony.json"), changed, 0o644); err != nil {
		t.Fatal(err)
	}
	if got := exit("build", "-check", "-data", dir, "-out", out); got != 1 {
		t.Errorf("data changed: exit %d, want 1", got)
	}
}
