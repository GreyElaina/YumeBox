package main

//#include "bridge.h"
import "C"

import (
	"unsafe"

	"github.com/metacubex/mihomo/adapter/outbound"
)

func init() {
	outbound.OnTailscaleNotify = func(snapshotJSON string) {
		C.tailscale_notify(C.CString(snapshotJSON))
	}
}

//export tailscaleExec
func tailscaleExec(completable unsafe.Pointer, request C.c_string) {
	go func(req string) {
		fn := outbound.TailscaleExecFunc
		if fn == nil {
			C.complete(completable, C.CString("tailscale not running"))
			return
		}
		result, err := fn(req)
		if err != nil {
			C.complete(completable, C.CString(err.Error()))
			return
		}
		C.complete_with_string(completable, C.CString(result))
	}(C.GoString(request))
}
