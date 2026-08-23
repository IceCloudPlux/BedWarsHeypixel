import base64

url = 'https://raw.githubusercontent.com/IceCloudPlux/IceCloudPlux-Website/main/_data/licenses.txt'

chunk_size = 5
chunks = [url[i:i+chunk_size] for i in range(0, len(url), chunk_size)]

enc_frag = []
for i, chunk in enumerate(chunks):
    key = (0x3C + i * 7) & 0xFF
    encrypted_bytes = bytes([b ^ key for b in chunk.encode('ascii')])
    b64 = base64.b64encode(encrypted_bytes).decode('ascii')
    enc_frag.append(f'"{b64}"')

# Print the Java array
lines = []
for i in range(0, len(enc_frag), 5):
    lines.append('        ' + ', '.join(enc_frag[i:i+5]) + ',')

print('ENCODED URL:')
print(url)
print()
print('Java array:')
print('private static final String[] ENC_FRAG = {')
for line in lines[:-1]:
    print(line)
print(lines[-1].rstrip(','))
print('};')

# Verify
decrypted = ''
for i, chunk in enumerate(chunks):
    key = (0x3C + i * 7) & 0xFF
    enc_bytes = base64.b64decode(enc_frag[i].strip('"'))
    decrypted += bytes([b ^ key for b in enc_bytes]).decode('ascii')
print()
print('Decrypted:', decrypted)
print('Match:', decrypted == url)
print()
print('URL length:', len(url), '| Chunks:', len(chunks))