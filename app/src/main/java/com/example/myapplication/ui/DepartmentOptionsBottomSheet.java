package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.R;
import com.example.myapplication.model.Department;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class DepartmentOptionsBottomSheet extends BottomSheetDialogFragment {

    private Department department;
    private OnOptionSelectedListener listener;

    public interface OnOptionSelectedListener {
        void onViewDetail(Department dept);
        void onEdit(Department dept);
        void onDelete(Department dept);
    }

    public static DepartmentOptionsBottomSheet newInstance(Department dept, OnOptionSelectedListener listener) {
        DepartmentOptionsBottomSheet fragment = new DepartmentOptionsBottomSheet();
        fragment.department = dept;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_department_options, container, false);

        if (department != null) {
            ((TextView) view.findViewById(R.id.tvDeptNameTitle)).setText(department.getName());
        }

        view.findViewById(R.id.btnViewDetail).setOnClickListener(v -> {
            if (listener != null) listener.onViewDetail(department);
            dismiss();
        });

        view.findViewById(R.id.btnEditDept).setOnClickListener(v -> {
            if (listener != null) listener.onEdit(department);
            dismiss();
        });

        view.findViewById(R.id.btnDeleteDept).setOnClickListener(v -> {
            if (listener != null) listener.onDelete(department);
            dismiss();
        });

        return view;
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
