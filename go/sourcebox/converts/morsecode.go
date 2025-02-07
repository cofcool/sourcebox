package converts

import (
	"sourcebox/tool"
	"strings"
)

var table = map[string]string{
	"A":  ".-",
	"B":  "-...",
	"C":  "-.-.",
	"D":  "-..",
	"E":  ".",
	"F":  "..-.",
	"G":  "--.",
	"H":  "....",
	"I":  "..",
	"J":  ".---",
	"K":  "-.-",
	"L":  ".-..",
	"M":  "--",
	"N":  "-.",
	"O":  "---",
	"P":  ".--.",
	"Q":  "--.-",
	"R":  ".-.",
	"S":  "...",
	"T":  "-",
	"U":  "..-",
	"V":  "...-",
	"W":  ".--",
	"X":  "-..-",
	"Y":  "-.--",
	"Z":  "--..",
	"1":  ".----",
	"2":  "..---",
	"3":  "...--",
	"4":  "....-",
	"5":  ".....",
	"6":  "-....",
	"7":  "--...",
	"8":  "---..",
	"9":  "----.",
	"0":  "-----",
	",":  "--..--",
	";":  "-.-.-.",
	":":  "---...",
	".":  ".-.-.-",
	"'":  ".----.",
	"\"": ".-..-.",
	"?":  "..--..",
	"/":  "-..-.",
	"-":  "-....-",
	"(":  "-.--.",
	")":  "-.--.-",
	"!":  "-.-.--",
	"$":  "...-..-",
	"@":  ".--.-.",
	"=":  "-...-",
}

var table_r = func() map[string]string {
	var tmp = make(map[string]string, len(table))
	for s, s2 := range table {
		tmp[s2] = s
	}

	return tmp
}()

func runMorseCode(args tool.Config) string {
	mtype, _ := args.ReadArg("mtype")
	in, _ := args.ReadArg("in")
	switch mtype.Val {
	case "en":
		tmp := strings.SplitAfter(in.Val, " ")
		var ret []string
		for _, v := range tmp {
			for _, s := range v {
				ret = append(ret, table[strings.ToUpper(string(s))])
			}
		}
		return strings.Join(ret, " ")
	case "de":
		tmp := strings.Split(in.Val, "  ")
		var ret []string
		for _, s := range tmp {
			var word []string
			for _, s2 := range strings.Split(s, " ") {
				word = append(word, table_r[s2])
			}
			ret = append(ret, strings.Join(word, ""))
		}
		return strings.Join(ret, " ")
	default:
		return in.Val
	}

	return ""
}
