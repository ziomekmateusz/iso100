// Command catalog validates, formats and builds the camera catalog in data/ and imports cameras from Lensfun.
//
// Exit codes: 0 ok, 1 errors or failed check, 2 wrong usage.
package main

import (
	"bytes"
	"errors"
	"flag"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"strings"

	"github.com/ziomekmateusz/iso100/tools/catalog"
	"github.com/ziomekmateusz/iso100/tools/lensfun"
)

const usage = `usage: catalog <command> [flags]

commands:
  validate                     check the catalog, print errors and warnings
  fmt [-check]                 rewrite the maker files in canonical form
  build [-out FILE] [-check]   validate and write catalog.json
  import-lensfun -lensfun DIR [-groups slr,mil,rf,om] [-tolerance 0.03] [-dry-run] [-v]

every command accepts -data DIR (default "data")`

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr))
}

func run(args []string, stdout, stderr io.Writer) int {
	if len(args) == 0 {
		fmt.Fprintln(stderr, usage)
		return 2
	}
	flags := flag.NewFlagSet("catalog "+args[0], flag.ContinueOnError)
	flags.SetOutput(stderr)
	data := flags.String("data", "data", "catalog directory")
	var cmd func() error
	switch args[0] {
	case "validate":
		cmd = func() error { return validate(*data, stdout) }
	case "fmt":
		check := flags.Bool("check", false, "fail if a file is not canonical, without writing")
		cmd = func() error { return format(*data, *check, stdout) }
	case "build":
		out := flags.String("out", "dist/catalog.json", "output file")
		check := flags.Bool("check", false, "fail if the output file is missing or different, without writing")
		cmd = func() error { return build(*data, *out, *check, stdout) }
	case "import-lensfun":
		dir := flags.String("lensfun", "", "Lensfun data/db directory")
		groups := flags.String("groups", strings.Join(lensfun.DefaultGroups, ","), "comma separated groups")
		tolerance := flags.Float64("tolerance", 0.03, "maximum relative crop factor difference")
		dryRun := flags.Bool("dry-run", false, "print the report without writing")
		verbose := flags.Bool("v", false, "also list the added cameras")
		cmd = func() error {
			if *dir == "" {
				return usageError("-lensfun is required")
			}
			return importLensfun(*data, *dir, strings.Split(*groups, ","), *tolerance, *dryRun, *verbose, stdout)
		}
	default:
		fmt.Fprintf(stderr, "unknown command %q\n\n%s\n", args[0], usage)
		return 2
	}
	if err := flags.Parse(args[1:]); err != nil {
		return 2
	}
	if flags.NArg() > 0 {
		fmt.Fprintf(stderr, "unexpected arguments: %s\n", strings.Join(flags.Args(), " "))
		return 2
	}
	if err := cmd(); err != nil {
		fmt.Fprintln(stderr, err)
		if errors.As(err, new(usageError)) {
			return 2
		}
		return 1
	}
	return 0
}

type usageError string

func (e usageError) Error() string { return string(e) }

func validate(data string, stdout io.Writer) error {
	cat, err := catalog.Load(data)
	if err != nil {
		return err
	}
	issues := cat.Validate()
	for _, i := range issues {
		fmt.Fprintln(stdout, i)
	}
	errs, warnings := catalog.Count(issues)
	if errs > 0 {
		return fmt.Errorf("failed: %d errors, %d warnings", errs, warnings)
	}
	cameras := 0
	for _, m := range cat.Makers {
		cameras += len(m.Cameras)
	}
	fmt.Fprintf(stdout, "ok: %d formats, %d makers, %d cameras, %d warnings\n", len(cat.Formats), len(cat.Makers), cameras, warnings)
	return nil
}

func format(data string, check bool, stdout io.Writer) error {
	cat, err := catalog.Load(data)
	if err != nil {
		return err
	}
	if !check {
		changed, err := cat.WriteMakers(data)
		for _, f := range changed {
			fmt.Fprintln(stdout, "formatted", f)
		}
		return err
	}
	var bad []string
	for _, m := range cat.Makers {
		want, err := m.Canonical()
		if err != nil {
			return err
		}
		if got, _ := os.ReadFile(filepath.Join(data, catalog.CamerasDir, m.File)); !bytes.Equal(got, want) {
			bad = append(bad, catalog.CamerasDir+"/"+m.File)
		}
	}
	if len(bad) > 0 {
		return fmt.Errorf("not canonical, run catalog fmt: %s", strings.Join(bad, ", "))
	}
	return nil
}

func build(data, out string, check bool, stdout io.Writer) error {
	cat, err := catalog.Load(data)
	if err != nil {
		return err
	}
	content, err := cat.Build()
	if err != nil {
		return err
	}
	if check {
		got, err := os.ReadFile(out)
		if errors.Is(err, fs.ErrNotExist) {
			return fmt.Errorf("%s is missing, run catalog build -out %s", out, out)
		}
		if err != nil {
			return err
		}
		if !bytes.Equal(got, content) {
			return fmt.Errorf("%s is out of date, run catalog build -out %s", out, out)
		}
		return nil
	}
	if err := os.MkdirAll(filepath.Dir(out), 0o755); err != nil {
		return err
	}
	if err := os.WriteFile(out, content, 0o644); err != nil {
		return err
	}
	fmt.Fprintf(stdout, "wrote %s\n", out)
	return nil
}

func importLensfun(data, dir string, groups []string, tolerance float64, dryRun, verbose bool, stdout io.Writer) error {
	cat, err := catalog.Load(data)
	if err != nil {
		return err
	}
	cams, skipped, err := lensfun.Read(dir, groups)
	if err != nil {
		return err
	}
	r := lensfun.Merge(cat, cams, tolerance)
	fmt.Fprintf(stdout, "read %d cameras from %s (groups %s, %d skipped)\n", r.Read, dir, strings.Join(groups, ","), skipped)
	fmt.Fprintf(stdout, "added %d, updated %d, unchanged %d, unmatched %d, duplicates %d, conflicts %d\n",
		len(r.Added), len(r.Updated), r.Unchanged, len(r.Unmatched), len(r.Duplicates), len(r.Conflicts))
	list := func(title string, items []string) {
		if len(items) > 0 {
			fmt.Fprintf(stdout, "%s:\n  %s\n", title, strings.Join(items, "\n  "))
		}
	}
	list("conflicts", r.Conflicts)
	list("duplicates", r.Duplicates)
	list("unmatched", r.Unmatched)
	list("updated", r.Updated)
	if verbose {
		list("added", r.Added)
	}
	issues := cat.Validate()
	if errs, _ := catalog.Count(issues); errs > 0 {
		for _, i := range issues {
			if i.Severity == catalog.Error {
				fmt.Fprintln(stdout, i)
			}
		}
		return fmt.Errorf("the merged catalog has %d errors, nothing written", errs)
	}
	if dryRun {
		return nil
	}
	changed, err := cat.WriteMakers(data)
	fmt.Fprintf(stdout, "wrote %d files\n", len(changed))
	return err
}
