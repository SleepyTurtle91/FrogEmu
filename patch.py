import sys
with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
    data = f.read()

bad_str = """    <Button
    <Button 
        android:id="@+id/btn_upscaler" 
        android:layout_width="wrap_content" 
        android:layout_height="wrap_content" 
        android:text="Toggle Shader" 
        android:layout_alignParentTop="true" 
        android:layout_alignParentRight="true" 
        android:layout_marginTop="10dp" 
        android:layout_marginRight="10dp" 
        android:background="#88000000" 
        android:textColor="#FFFFFF"/>
        android:id="@+id/btn_load_rom\""""
        
good_str = """    <Button 
        android:id="@+id/btn_upscaler" 
        android:layout_width="wrap_content" 
        android:layout_height="wrap_content" 
        android:text="Toggle Shader" 
        android:layout_alignParentTop="true" 
        android:layout_alignParentRight="true" 
        android:layout_marginTop="10dp" 
        android:layout_marginRight="10dp" 
        android:background="#88000000" 
        android:textColor="#FFFFFF"/>
        
    <Button 
        android:id="@+id/btn_load_rom\""""

data = data.replace(bad_str, good_str)
with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
    f.write(data)
