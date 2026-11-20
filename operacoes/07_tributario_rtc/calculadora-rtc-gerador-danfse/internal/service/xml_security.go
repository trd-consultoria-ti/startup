package service

import (
	"encoding/xml"
	"io"
	"strings"
)

// MaxXMLInputSize is the maximum allowed size for XML input (5 MiB).
const MaxXMLInputSize = 5 * 1024 * 1024

// MaxXMLInputSizeLimit is the hard ceiling (32 MiB) for a configurable XML
// input size limit. It exists to prevent accidental values that effectively
// disable the protection. The default remains MaxXMLInputSize.
const MaxXMLInputSizeLimit = 32 * 1024 * 1024

// RejectDTDAndEntities scans the raw XML bytes for DOCTYPE or ENTITY declarations.
// It accepts the XML declaration and namespace declarations, but rejects any
// directive containing DOCTYPE or ENTITY (case-insensitive).
// It does not configure custom entity resolution.
func RejectDTDAndEntities(xmlData []byte) error {
	dec := xml.NewDecoder(strings.NewReader(string(xmlData)))
	// Do not set any custom entity handler — encoding/xml does not resolve
	// external entities by default, and we must not change that.

	for {
		tok, err := dec.Token()
		if err != nil {
			if err == io.EOF {
				return nil
			}
			return err
		}
		switch v := tok.(type) {
		case xml.Directive:
			directive := strings.ToUpper(string(v))
			if strings.Contains(directive, "DOCTYPE") || strings.Contains(directive, "ENTITY") {
				return ErrXMLContainsDTD
			}
		}
	}
}

// ErrXMLContainsDTD is returned when the XML contains a DOCTYPE or ENTITY declaration.
var ErrXMLContainsDTD = &XMLSecurityError{reason: "XML contains DTD or entity declaration"}

// XMLSecurityError represents a security-related validation failure on XML input.
type XMLSecurityError struct {
	reason string
}

func (e *XMLSecurityError) Error() string {
	return e.reason
}
