package catalog

import "strings"

// transliteration of the non-ASCII letters that appear in camera and format names
var translit = map[rune]string{
	'à': "a", 'á': "a", 'â': "a", 'ã': "a", 'ä': "a", 'å': "a", 'ą': "a",
	'ç': "c", 'ć': "c", 'č': "c",
	'è': "e", 'é': "e", 'ê': "e", 'ë': "e", 'ę': "e",
	'ì': "i", 'í': "i", 'î': "i", 'ï': "i",
	'ł': "l", 'ñ': "n", 'ń': "n",
	'ò': "o", 'ó': "o", 'ô': "o", 'õ': "o", 'ö': "o", 'ø': "o",
	'ś': "s", 'š': "s", 'ù': "u", 'ú': "u", 'û': "u", 'ü': "u",
	'ý': "y", 'ÿ': "y", 'ź': "z", 'ż': "z", 'ž': "z",
	'ß': "ss", 'æ': "ae", 'œ': "oe", 'µ': "u", '×': "x",
}

// Slug returns s in lower case ASCII with words separated by "-": "Sony Alpha 7 III" → "sony-alpha-7-iii".
func Slug(s string) string {
	var b strings.Builder
	gap := false
	for _, r := range strings.ToLower(s) {
		t := translit[r]
		if r < 0x80 && (r >= 'a' && r <= 'z' || r >= '0' && r <= '9') {
			t = string(r)
		}
		if t == "" {
			gap = true
			continue
		}
		if gap && b.Len() > 0 {
			b.WriteByte('-')
		}
		gap = false
		b.WriteString(t)
	}
	return b.String()
}

func validID(id string) bool {
	return id != "" && Slug(id) == id
}
