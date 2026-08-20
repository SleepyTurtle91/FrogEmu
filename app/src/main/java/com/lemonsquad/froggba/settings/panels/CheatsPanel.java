package com.lemonsquad.froggba.settings.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import com.lemonsquad.froggba.R;
import com.lemonsquad.froggba.cheats.CheatItem;
import com.lemonsquad.froggba.cheats.CheatRepository;
import com.lemonsquad.froggba.cheats.RomMatcher;
import com.lemonsquad.froggba.settings.FrogEmuSettings;
import com.lemonsquad.froggba.settings.SettingsDialog;
import com.lemonsquad.froggba.settings.SettingsPanel;

public class CheatsPanel implements SettingsPanel {

    private final CheatRepository mCheatRepo;

    public CheatsPanel(CheatRepository cheatRepo) {
        mCheatRepo = cheatRepo;
    }

    @Override
    public String getId() { return "cheats"; }

    @Override
    public String getTitle() { return "Cheats Engine"; }

    @Override
    public String getIcon() { return "🧪"; }

    @Override
    public View createView(Context context, LayoutInflater inflater, ViewGroup container,
                           FrogEmuSettings settings, SettingsDialog.OnSettingsChangedListener listener) {
        View view = inflater.inflate(R.layout.panel_settings_cheats, container, false);

        TextView txtTitle = view.findViewById(R.id.txt_cheat_game_title);
        TextView txtCode = view.findViewById(R.id.txt_cheat_game_code);
        TextView txtStatus = view.findViewById(R.id.txt_cheat_db_status);
        TextView txtCount = view.findViewById(R.id.txt_cheats_count);
        TextView txtEmpty = view.findViewById(R.id.txt_no_cheats);
        LinearLayout listContainer = view.findViewById(R.id.layout_cheats_list);
        View btnDisableAll = view.findViewById(R.id.btn_disable_all_cheats);

        if (mCheatRepo == null) {
            txtTitle.setText("Cheats Engine Standby");
            txtCode.setText("IDLE");
            txtEmpty.setVisibility(View.VISIBLE);
            return view;
        }

        RomMatcher.RomMetadata activeRom = mCheatRepo.getActiveRom();
        if (activeRom != null) {
            txtTitle.setText(activeRom.title);
            txtCode.setText(activeRom.gameCode);
            txtStatus.setText(String.format("CRC32: %08X • Version 1.%d", activeRom.crc32, activeRom.version));
        } else {
            txtTitle.setText("No ROM Loaded");
            txtCode.setText("NONE");
            txtStatus.setText("Database: Waiting for game");
        }

        btnDisableAll.setOnClickListener(v -> {
            List<CheatItem> cheats = mCheatRepo.getCurrentCheats();
            for (int i = 0; i < cheats.size(); ++i) {
                mCheatRepo.setCheatEnabled(i, false);
            }
            refreshList(context, inflater, listContainer, txtCount, txtEmpty);
        });

        refreshList(context, inflater, listContainer, txtCount, txtEmpty);
        return view;
    }

    private void refreshList(Context context, LayoutInflater inflater,
                             LinearLayout container, TextView txtCount, TextView txtEmpty) {
        container.removeAllViews();
        if (mCheatRepo == null) return;

        List<CheatItem> cheats = mCheatRepo.getCurrentCheats();
        txtCount.setText("Available Cheats (" + cheats.size() + ")");

        if (cheats.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }
        txtEmpty.setVisibility(View.GONE);

        for (int i = 0; i < cheats.size(); ++i) {
            final int index = i;
            CheatItem item = cheats.get(i);

            View row = inflater.inflate(R.layout.item_cheat_row, container, false);
            TextView title = row.findViewById(R.id.txt_cheat_title);
            TextView meta = row.findViewById(R.id.txt_cheat_meta);
            CheckBox chk = row.findViewById(R.id.chk_cheat_toggle);

            title.setText(item.getName());
            if (item.isMasterCode()) {
                title.setTextColor(0xFF00FF66); // Green highlight for Master Codes
                meta.setText("⚡ Master Code (Required for dependent cheats) • " + item.getCodes().size() + " lines");
            } else {
                title.setTextColor(0xFFFFFFFF);
                meta.setText(item.getProvider() + " • " + item.getCodes().size() + " lines");
            }

            chk.setChecked(item.isEnabled());

            chk.setOnClickListener(v -> {
                boolean isChecked = chk.isChecked();
                boolean success = mCheatRepo.setCheatEnabled(index, isChecked);
                if (!success) {
                    chk.setChecked(true);
                    Toast.makeText(context, "Cannot disable Master Code while other cheats are active!", Toast.LENGTH_SHORT).show();
                } else {
                    refreshList(context, inflater, container, txtCount, txtEmpty);
                }
            });

            row.setOnClickListener(v -> {
                chk.setChecked(!chk.isChecked());
                boolean isChecked = chk.isChecked();
                boolean success = mCheatRepo.setCheatEnabled(index, isChecked);
                if (!success) {
                    chk.setChecked(true);
                    Toast.makeText(context, "Cannot disable Master Code while other cheats are active!", Toast.LENGTH_SHORT).show();
                } else {
                    refreshList(context, inflater, container, txtCount, txtEmpty);
                }
            });

            container.addView(row);
        }
    }
}
