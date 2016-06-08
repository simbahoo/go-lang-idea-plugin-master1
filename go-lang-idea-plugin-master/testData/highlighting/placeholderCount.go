package placeholderCount

import "fmt"
import "log"
import "testing"

const (
	myFormatConst      = "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d"
	myWrongFormatConst = "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d"
)

func printf(string, ...int) {

}

var s string = "%d"
const s1 = <error descr="Cyclic definition detected">s1</error>
const s2 = 1

func _(t *testing.T) {
	fmt.Printf(<warning descr="Got 1 placeholder(s) for 2 arguments(s)">s</warning>, 1, 2)

	fmt.Printf(<warning descr="Value used for formatting text does not appear to be a string">s1</warning>, 1, 2)

	fmt.Printf(<warning descr="Value used for formatting text does not appear to be a string">s2</warning>, 1, 2)

	fmt.Errorf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Fprintf(nil, "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Fscanf(nil, "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Printf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Scanf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Sprintf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	fmt.Sscanf(nil, "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	log.Fatalf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	log.Panicf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)
	log.Printf("%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d",
		1, 2, 3, 4, 5, 6, 7, 8, 9,
	)

	fmt.Printf("eq (as-is): %.3f%% score: %v offset: %v descr: [%v]\n", 3.14, 1, 1, "descr")

	fmt.Printf("a: %+v", 1)

	fmt.Printf("%-4d", 999)

	fmt.Printf("a: %%%+v", 1)

	fmt.Printf(<warning descr="Got 0 placeholder(s) for 1 arguments(s)">"a: %%%%+v"</warning>, 1)

	fmt.Printf("#%02X%02X%02X", 1, 2, 3)

	fmt.Printf(<warning descr="Got 3 placeholder(s) for 4 arguments(s)">"#%02X%02X%02X"</warning>, 1, 2, 3, 4)

	myFormatVar := "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d"
	log.Printf(myFormatVar, 1, 2, 3, 4, 5, 6, 7, 8, 9)

	myWrongFormatVar := "%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d"
	log.Printf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatVar</warning>, 1, 2, 3, 4, 5, 6, 7, 8)

	log.Printf(myFormatConst, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	t.Errorf(myFormatConst, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	t.Fatalf(myFormatConst, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	t.Logf(myFormatConst, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	t.Skipf(myFormatConst, 1, 2, 3, 4, 5, 6, 7, 8, 9)

	log.Printf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatConst</warning>, 1, 2, 3, 4, 5, 6, 7, 8)
	t.Errorf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatConst</warning>, 1, 2, 3, 4, 5, 6, 7, 8)
	t.Fatalf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatConst</warning>, 1, 2, 3, 4, 5, 6, 7, 8)
	t.Logf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatConst</warning>, 1, 2, 3, 4, 5, 6, 7, 8)
	t.Skipf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">myWrongFormatConst</warning>, 1, 2, 3, 4, 5, 6, 7, 8)

	printf("%d", 1)
	printf("%[2]d %[1]d", 1, 2)
	printf("%[2]d %[1]d %d", 1, 2)
	printf("%[2]d %[1]d %[2]d", 1, 2)
	printf("%d")

	myNonFormatFunc := func () int {
		return 1
	}
	log.Printf(<warning descr="Value used for formatting text does not appear to be a string">myNonFormatFunc()</warning>, 1, 2, 3, 4, 5, 6, 7, 8, 9)

	log.Printf(<warning descr="Got 9 placeholder(s) for 8 arguments(s)">"%d %d %#[1]x %#x %2.f %d %2.2f %.f %.3f %[9]*.[2]*[3]f %d %f %#[1]x %#x %[2]d %v % d"</warning>,
		1, 2, 3, 4, 5, 6, 7, 8,
	)
	fmt.Sprintf(<warning descr="Got 1 placeholder(s) for 0 arguments(s)">"%d"</warning>)

	log.Printf(<warning descr="Got 7 placeholder(s) for 13 arguments(s)">"%d %d %#[1]x %#x %f %2.f %2.2f %.f %.3f %[3]*.[2]*[1]f %d %d %#[1]x %#x %*[2]d %v % d"</warning>,
		1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
	)
	fmt.Sprintf(<warning descr="Got 1 placeholder(s) for 2 arguments(s)">"%d"</warning>, 1, 2)
}
