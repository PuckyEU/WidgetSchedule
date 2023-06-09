package lukas.sobotik.widgetschedule.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import lukas.sobotik.widgetschedule.*;
import lukas.sobotik.widgetschedule.adapter.ItemColorAdapter;
import lukas.sobotik.widgetschedule.database.SettingsDatabaseHelper;
import lukas.sobotik.widgetschedule.entity.Settings;
import lukas.sobotik.widgetschedule.entity.SettingsEntry;

import java.util.*;

public class ColorFragment extends Fragment {
    SettingsDatabaseHelper settingsDatabaseHelper;
    ListView itemColorListView;
    TextInputLayout itemColors;
    Button addColorButton;

    List<String> list;

    public ColorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_color, container, false);

        settingsDatabaseHelper = new SettingsDatabaseHelper(getContext());

        itemColors = inflatedView.findViewById(R.id.color_text_input);
        itemColorListView = inflatedView.findViewById(R.id.color_list_view);
        addColorButton = inflatedView.findViewById(R.id.color_add_button);

        list = new ArrayList<>();
        loadDataFromDatabase();

        ItemColorAdapter.EditTextChangeListener editTextChangeListener = new ItemColorAdapter.EditTextChangeListener() {
            @Override
            public void onTextChanged(int position, String newText) {
                list.set(position, newText);
                scheduleSaveOperation();
            }
        };

        ItemColorAdapter colorAdapter = new ItemColorAdapter(getContext(), list);
        colorAdapter.setEditTextChangeListener(editTextChangeListener);
        itemColorListView.setAdapter(colorAdapter);

        itemColorListView.setOnItemLongClickListener((parent, view, position, id) -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete item?")
                    .setMessage("Are you sure you want to delete this item?")
                    .setNegativeButton("Cancel", (dialog, which) -> {

                    })
                    .setPositiveButton("Delete", (dialog, which) -> {
                        list.remove(position);
                        colorAdapter.notifyDataSetChanged();
                        scheduleSaveOperation();
                    })
                    .show();
            return true;
        });

        addColorButton.setOnClickListener(view -> {
            String itemColorsSource = Objects.requireNonNull(itemColors.getEditText()).getText().toString().toLowerCase().trim();
            if (!itemColorsSource.isEmpty()) {
                String[] filteredArray = Arrays.stream(itemColorsSource.split(";"))
                        .filter(item -> !item.isEmpty())
                        .toArray(String[]::new);
                Collections.addAll(list, filteredArray);
                colorAdapter.notifyDataSetChanged();
                itemColors.getEditText().setText("");
            }
            scheduleSaveOperation();
        });

        return inflatedView;
    }

    private void saveDataToDatabase() {
        StringBuilder builder = new StringBuilder();
        for (String item : list) {
            builder.append(item).append(";");
        }
        Log.d("Custom Logging", "Saving: " + builder);

        settingsDatabaseHelper.addItem(new SettingsEntry(new Settings().ItemColors, builder.toString()));
        Snackbar.make(requireView(), "Saved", Snackbar.LENGTH_SHORT).show();
    }

    TimerTask saveTask = new TimerTask() {
        @Override
        public void run() {
            saveDataToDatabase();
        }
    };

    private void scheduleSaveOperation() {
        Timer timer = new Timer();
        if (saveTask != null) saveTask.cancel();
        saveTask = new TimerTask() {
            @Override
            public void run() {
                saveDataToDatabase();
            }
        };
        timer.schedule(saveTask, 2000);
    }

    private void loadDataFromDatabase() {
        Cursor settingsCursor = settingsDatabaseHelper.readAllData();
        if (settingsCursor.getCount() == 0) {
            return;
        }

        while (settingsCursor.moveToNext()) {
            if (Objects.equals(settingsCursor.getString(1), String.valueOf(new Settings().ItemColors))) {
                String allItems = settingsCursor.getString(2);
                String[] filteredArray = Arrays.stream(allItems.split(";"))
                        .filter(item -> !item.isEmpty())
                        .toArray(String[]::new);
                Collections.addAll(list, filteredArray);
            }
        }
    }
}