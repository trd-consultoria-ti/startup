import sys
try:
    import pytesseract
    print('pytesseract available')
except ImportError:
    print('pytesseract not available')
