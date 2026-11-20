package service

import (
	"strings"
	"testing"
)

func TestRejectDTDAndEntities_DTDRejection(t *testing.T) {
	xml := `<?xml version="1.0"?><!DOCTYPE foo SYSTEM "http://evil.com/evil.dtd"><NFSe><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err == nil {
		t.Fatal("expected error for DOCTYPE, got nil")
	}
	if !strings.Contains(err.Error(), "DTD") {
		t.Fatalf("expected DTD error, got: %v", err)
	}
}

func TestRejectDTDAndEntities_EntityDeclarationRejection(t *testing.T) {
	xml := `<?xml version="1.0"?><!ENTITY foo "bar"><NFSe><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err == nil {
		t.Fatal("expected error for ENTITY, got nil")
	}
	if !strings.Contains(err.Error(), "entity") {
		t.Fatalf("expected entity error, got: %v", err)
	}
}

func TestRejectDTDAndEntities_CaseInsensitive(t *testing.T) {
	xml := `<?xml version="1.0"?><!doctype foo><NFSe><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err == nil {
		t.Fatal("expected error for lowercase doctype, got nil")
	}
}

func TestRejectDTDAndEntities_ValidXML(t *testing.T) {
	xml := `<?xml version="1.0" encoding="UTF-8"?><NFSe xmlns="http://www.sped.fazenda.gov.br/nfse" versao="1.01"><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err != nil {
		t.Fatalf("expected no error for valid XML, got: %v", err)
	}
}

func TestRejectDTDAndEntities_XMLDeclarationAllowed(t *testing.T) {
	xml := `<?xml version="1.0" encoding="UTF-8"?><NFSe><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err != nil {
		t.Fatalf("expected no error for XML declaration, got: %v", err)
	}
}

func TestRejectDTDAndEntities_NamespaceAllowed(t *testing.T) {
	xml := `<NFSe xmlns="http://www.sped.fazenda.gov.br/nfse" xmlns:ns2="http://example.com"><infNFSe Id="NFS123"></infNFSe></NFSe>`
	err := RejectDTDAndEntities([]byte(xml))
	if err != nil {
		t.Fatalf("expected no error for namespace declarations, got: %v", err)
	}
}

func TestXMLSecurityError(t *testing.T) {
	err := ErrXMLContainsDTD
	if err.Error() != "XML contains DTD or entity declaration" {
		t.Fatalf("unexpected error message: %s", err.Error())
	}
}

func TestRejectDTDAndEntities_DecoderErrorNotSwallowed(t *testing.T) {
	// Malformed XML that causes a decoder error (not EOF).
	xml := `<?xml version="1.0"?><NFSe><infNFSe Id="NFS123"><unclosed>`
	err := RejectDTDAndEntities([]byte(xml))
	if err == nil {
		t.Fatal("expected decoder error for malformed XML, got nil")
	}
}
