package controller

import (
	"sync"
	"testing"
)

func TestReadiness_EstadoInicialEFlips(t *testing.T) {
	r := NewReadiness(true)
	if !r.IsReady() {
		t.Fatal("esperava ready=true inicialmente")
	}
	r.SetReady(false)
	if r.IsReady() {
		t.Fatal("esperava ready=false após SetReady(false)")
	}
	r.SetReady(true)
	if !r.IsReady() {
		t.Fatal("esperava ready=true após SetReady(true)")
	}
}

func TestReadiness_NilEhPronto(t *testing.T) {
	var r *Readiness
	if !r.IsReady() {
		t.Fatal("readiness nil deveria ser tratado como pronto")
	}
	r.SetReady(false) // não deve panic
}

func TestReadiness_ThreadSafe(t *testing.T) {
	r := NewReadiness(true)
	var wg sync.WaitGroup
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			if i%2 == 0 {
				r.SetReady(true)
			} else {
				r.SetReady(false)
			}
			_ = r.IsReady()
		}(i)
	}
	wg.Wait()
	// apenas garante que não houve data race durante os flips.
}
