import os
import pefile

exe_path = "raw_assets/Ikachan.exe"
out_dir = "raw_assets/sfx_raw"
os.makedirs(out_dir, exist_ok=True)

pe = pefile.PE(exe_path)
for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    res_type = str(entry.name) if entry.name else str(entry.id)
    for item in entry.directory.entries:
        name = str(item.name) if item.name else str(item.id)
        data = item.directory.entries[0].data.struct
        raw = pe.get_data(data.OffsetToData, data.Size)
        if res_type.upper() == "WAVE" or raw[:4] == b"RIFF":
            out_path = os.path.join(out_dir, f"{name}.wav")
            with open(out_path, "wb") as f:
                f.write(raw)
            print(f"Extracted {out_path} ({data.Size} bytes)")