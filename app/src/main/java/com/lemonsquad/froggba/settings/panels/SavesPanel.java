package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.cheats.RomMatcher;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;
import com.lemonsquad.froggba.states.SaveStateManager;

public class SavesPanel implements SettingsPanel {

    private final SaveStateManager mStateManager;
    private final RomMatcher.RomMetadata mActiveRom;

    public SavesPanel(SaveStateManager stateManager, RomMatcher.RomMetadata activeRom) {
        mStateManager = stateManager;
        mActiveRom = activeRom;
    }

    @Override
    public String getId() { return "saves"; }

    @Override
    public String getTitle() { return "Saves & State"; }

    @Override
    public String getIcon() { return "💾"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_saves, container, false);

        TextView txtTitle = view.findViewById(R.id.txt_state_game_title);
        TextView txtCode = view.findViewById(R.id.txt_state_game_code);
        TextView txtPath = view.findViewById(R.id.txt_state_storage_path);
        LinearLayout slotsContainer = view.findViewById(R.id.layout_slots_container);

        String gameId = (mActiveRom != null && mActiveRom.gameCode != null && !mActiveRom.gameCode.isEmpty())
                ? mActiveRom.gameCode : "DEFAULT";

        if (mActiveRom != null) {
            txtTitle.setText(mActiveRom.title);
            txtCode.setText(mActiveRom.gameCode);
        } else {
            txtTitle.setText("No ROM Loaded");
            txtCode.setText("IDLE");
        }

        txtPath.setText("States Path: " + context.getFilesDir().getAbsolutePath() + "/states/");

        refreshSlots(context, inflater, slotsContainer, gameId);
        return view;
    }

    private void refreshSlots(Context context, LayoutInflater inflater,
                              LinearLayout container, String gameId) {
        container.removeAllViews();
        if (mStateManager == null) return;

        for (int i = 0; i < SaveStateManager.NUM_SLOTS; ++i) {
            final int slot = i;
            SaveStateManager.SlotInfo info = mStateManager.getSlotInfo(gameId, slot);

            View row = inflater.inflate(R.layout.item_save_slot, container, false);
            TextView title = row.findViewById(R.id.txt_slot_title);
            TextView sub = row.findViewById(R.id.txt_slot_info);
            Button btnSave = row.findViewById(R.id.btn_slot_save);
            Button btnLoad = row.findViewById(R.id.btn_slot_load);
            Button btnDelete = row.findViewById(R.id.btn_slot_delete);

            String slotName = (slot == 0) ? "Slot 0 (Quick Save)" : ("Slot " + slot);
            title.setText(slotName);

            if (info.exists) {
                sub.setText(info.formattedDate + " • " + (info.sizeBytes / 1024) + " KB");
                sub.setTextColor(0xFF00FF66);
                btnLoad.setEnabled(true);
                btnLoad.setAlpha(1.0f);
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                sub.setText("Empty Slot");
                sub.setTextColor(0xFF777777);
                btnLoad.setEnabled(false);
                btnLoad.setAlpha(0.4f);
                btnDelete.setVisibility(View.GONE);
            }

            btnSave.setOnClickListener(v -> {
                btnSave.setEnabled(false);
                mStateManager.saveSlot(gameId, slot, (success, message) -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    refreshSlots(context, inflater, container, gameId);
                });
            });

            btnLoad.setOnClickListener(v -> {
                btnLoad.setEnabled(false);
                mStateManager.loadSlot(gameId, slot, (success, message) -> {
                    btnLoad.setEnabled(true);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                });
            });

            btnDelete.setOnClickListener(v -> {
                boolean deleted = mStateManager.deleteSlot(gameId, slot);
                if (deleted) {
                    Toast.makeText(context, "Slot " + slot + " state deleted.", Toast.LENGTH_SHORT).show();
                    refreshSlots(context, inflater, container, gameId);
                }
            });

            container.addView(row);
        }
    }
}
