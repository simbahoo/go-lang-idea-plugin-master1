package main

type T1 struct {
	/*def*/t1Value int
}

type T2 struct {
	T1
}

type T3 struct {
	T2
}

type T4 struct {
	T3
}

func foo(t4 T4) {
    t4./*ref*/t1Value
}
