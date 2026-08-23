import zipfile
import struct

def validate_class(class_data):
    """Validate that a class file is structurally correct."""
    if len(class_data) < 32:
        return False
    
    magic = struct.unpack_from('>H', class_data, 0)[0]
    if magic != 0xCAFE:
        return False
    
    cp_count = struct.unpack_from('>H', class_data, 8)[0]
    if cp_count < 1:
        return False
    
    offset = 10
    i = 1
    while i < cp_count:
        if offset >= len(class_data):
            print(f"  Offset overflow at i={i}, cp_count={cp_count}")
            return False
        tag = class_data[offset]
        offset += 1
        
        if tag == 1:
            if offset + 2 > len(class_data):
                return False
            length = struct.unpack_from('>H', class_data, offset)[0]
            offset += 2 + length
        elif tag in (7, 8, 16, 17, 18, 19, 20, 21, 22):
            if offset + 2 > len(class_data):
                return False
            offset += 2
        elif tag in (9, 10, 11, 12):
            if offset + 4 > len(class_data):
                return False
            offset += 4
        elif tag in (3, 4):
            if offset + 4 > len(class_data):
                return False
            offset += 4
        elif tag in (5, 6):
            if offset + 8 > len(class_data):
                return False
            offset += 8
            i += 1
        elif tag == 15:
            if offset + 3 > len(class_data):
                return False
            offset += 3
        else:
            print(f"  Unknown tag {tag} at i={i}")
            return False
        
        i += 1
    
    return True


jar_path = r"c:\Users\Admin\Desktop\BedWars\BedWars1058-master\bedwars-plugin\target\bedwars-plugin-26.7.jar"

with zipfile.ZipFile(jar_path, 'r') as z:
    # Test a specific failing class
    test_files = [
        "com/andrei1058/bedwars/metrics/MetricsManager.class",
        "com/andrei1058/bedwars/utils/LicenseChecker.class",
        "com/andrei1058/bedwars/BedWars.class"
    ]
    for f in test_files:
        data = z.read(f)
        valid = validate_class(data)
        print(f"  {f}: {'VALID' if valid else 'INVALID'} (size={len(data)})")