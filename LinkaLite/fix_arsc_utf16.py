import struct
import zipfile
import os
import sys

def convert_string_pool_to_utf16(data):
    if len(data) < 28:
        return data
    chunk_type, header_size, chunk_size, string_count, style_count, flags, strings_start, styles_start = struct.unpack_from('<HHIIIIII', data, 0)
    
    if chunk_type != 1 or not (flags & 0x0100):
        return data

    offsets = struct.unpack_from(f'<{string_count}I', data, 28)
    strings = []
    
    for i in range(string_count):
        offset = offsets[i]
        idx = strings_start + offset
        if idx >= len(data):
            break
        
        u16_len = data[idx]
        if u16_len & 0x80:
            u16_len = ((u16_len & 0x7F) << 8) | data[idx+1]
            idx += 2
        else:
            idx += 1
            
        u8_len = data[idx]
        if u8_len & 0x80:
            u8_len = ((u8_len & 0x7F) << 8) | data[idx+1]
            idx += 2
        else:
            idx += 1
            
        str_bytes = data[idx : idx + u8_len]
        strings.append(str_bytes.decode('utf-8', errors='ignore'))

    new_flags = flags & ~0x0100
    new_offsets = []
    new_string_bytes = bytearray()

    for s in strings:
        new_offsets.append(len(new_string_bytes))
        encoded = s.encode('utf-16le')
        char_len = len(s)
        if char_len < 0x8000:
            len_bytes = struct.pack('<H', char_len)
        else:
            len_bytes = struct.pack('<H', (char_len >> 16) | 0x8000) + struct.pack('<H', char_len & 0xFFFF)
        
        new_string_bytes.extend(len_bytes + encoded + b'\x00\x00')

    new_strings_start = 28 + (string_count * 4) + (style_count * 4)
    style_data = b''
    new_styles_start = styles_start

    if style_count > 0 and styles_start > 0:
        style_data = data[styles_start:chunk_size]
        pad = (4 - (len(new_string_bytes) % 4)) % 4
        new_string_bytes.extend(b'\x00' * pad)
        new_styles_start = new_strings_start + len(new_string_bytes)
    else:
        pad = (4 - (len(new_string_bytes) % 4)) % 4
        new_string_bytes.extend(b'\x00' * pad)

    new_chunk_size = new_strings_start + len(new_string_bytes) + len(style_data)
    header = struct.pack('<HHIIIIII', 1, 28, new_chunk_size, string_count, style_count, new_flags, new_strings_start, new_styles_start)
    offsets_bin = struct.pack(f'<{string_count}I', *new_offsets)
    
    return header + offsets_bin + new_string_bytes + style_data

def patch_arsc_bytes(buffer):
    patched_arsc = bytearray(buffer)
    if len(patched_arsc) < 12:
        return bytes(patched_arsc)

    top_type, top_header_size, top_size = struct.unpack_from('<HHI', patched_arsc, 0)
    offset = top_header_size
    delta_total = 0

    while offset < len(patched_arsc) - 8:
        chunk_type, header_size, chunk_size = struct.unpack_from('<HHI', patched_arsc, offset)
        if chunk_type == 1:
            chunk_data = patched_arsc[offset : offset + chunk_size]
            converted = convert_string_pool_to_utf16(chunk_data)
            delta = len(converted) - chunk_size
            patched_arsc[offset : offset + chunk_size] = converted
            delta_total += delta
            offset += len(converted)
        else:
            offset += max(chunk_size, 4)

    if top_type == 2 and delta_total != 0:
        new_top_size = top_size + delta_total
        struct.pack_into('<I', patched_arsc, 4, new_top_size)

    return bytes(patched_arsc)

def patch_apk(apk_path, output_path):
    print(f"🔧 Limpando assinaturas velhas e corrigindo resources.arsc em: {apk_path}")
    tmp_apk = output_path + ".tmp"
    
    with zipfile.ZipFile(apk_path, 'r') as zin, zipfile.ZipFile(tmp_apk, 'w') as zout:
        for item in zin.infolist():
            # Remove assinaturas velhas para evitar conflitos no Android 1.5
            if item.filename.startswith("META-INF/"):
                continue
            
            buffer = zin.read(item.filename)
            if item.filename == "resources.arsc":
                buffer = patch_arsc_bytes(buffer)
            
            zout.writestr(item.filename, buffer, compress_type=item.compress_type)
            
    os.replace(tmp_apk, output_path)
    print("✅ APK limpo e corrigido com sucesso!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python3 fix_arsc_utf16.py <caminho_do_apk>")
        sys.exit(1)
    patch_apk(sys.argv[1], sys.argv[1])
