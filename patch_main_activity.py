import sys

filepath = 'app/src/main/java/com/lemonsquad/froggba/MainActivity.java'

with open(filepath, 'r') as f:
    content = f.read()

# Add a boolean or int for upscaler state
state_var = "    private int mUpscalerState = 0;\n"
if "mUpscalerState" not in content:
    content = content.replace("private boolean mIsEmulatorRunning = false;", "private boolean mIsEmulatorRunning = false;\n" + state_var)

# Add button click listener
button_code = """
        Button btnUpscaler = findViewById(R.id.btn_upscaler);
        btnUpscaler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUpscalerState = (mUpscalerState + 1) % 2;
                if (mUpscalerState == 0) {
                    mRenderer.setUpscaler(EmulatorRenderer.Upscaler.NEAREST);
                    Log.d("FroggBA_Shader", "Switched to NEAREST");
                } else {
                    mRenderer.setUpscaler(EmulatorRenderer.Upscaler.SCALE2X);
                    Log.d("FroggBA_Shader", "Switched to SCALE2X");
                }
            }
        });
"""

if "btnUpscaler.setOnClickListener" not in content:
    content = content.replace("Button btnLoad = findViewById(R.id.btn_load_rom);", button_code + "\n        Button btnLoad = findViewById(R.id.btn_load_rom);")

with open(filepath, 'w') as f:
    f.write(content)
