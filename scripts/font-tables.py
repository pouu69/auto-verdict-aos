#!/usr/bin/env python3
"""Print the OpenType table tags of a font file (no third-party deps)."""
import struct
import sys

def tables(path):
    with open(path, "rb") as f:
        data = f.read()
    num_tables = struct.unpack(">H", data[4:6])[0]
    return sorted(data[12 + 16 * i: 16 + 16 * i].decode("latin-1") for i in range(num_tables))

if __name__ == "__main__":
    for p in sys.argv[1:]:
        print(p, tables(p))
