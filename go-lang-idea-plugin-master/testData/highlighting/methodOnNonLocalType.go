package demo2031

import (
	. "fmt"
	"time"
)

type Duration time.Duration

func (_ *string) <error descr="Method 'Demo' defined on non-local type">Demo</error>() {

}

func (_ int) <error descr="Method 'Demo' defined on non-local type">Demo</error>() {

}

func (_ ScanState) <error descr="Method 'Demo' defined on non-local type">Demo</error>() {

}

func (_ *Duration) UnmarshalText(data []byte) (error) {
	_ = data
	return nil
}