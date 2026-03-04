package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_demo);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Nút quay lại

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tạo dữ liệu giả lập (Demo)
        employeeList = new ArrayList<>();
        employeeList.add(new Employee("Nguyễn Văn A", "Trưởng phòng IT", "Công nghệ thông tin", true));
        employeeList.add(new Employee("Trần Thị B", "Nhân viên Marketing", "Marketing", true));
        employeeList.add(new Employee("Lê Văn C", "Kế toán trưởng", "Kế toán", false));
        employeeList.add(new Employee("Phạm Thị D", "Nhân viên Nhân sự", "Nhân sự", true));
        employeeList.add(new Employee("Hoàng Văn E", "Designer", "Thiết kế", true));

        // Khởi tạo Adapter và gán sự kiện click hiển thị Dialog
        adapter = new EmployeeAdapter(employeeList, employee -> showEmployeeDialog(employee));
        recyclerView.setAdapter(adapter);
    }

    // Hàm hiển thị BottomSheetDialog
    private void showEmployeeDialog(Employee employee) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_detail, null);

        TextView tvAvatar = view.findViewById(R.id.tvDialogAvatar);
        TextView tvName = view.findViewById(R.id.tvDialogName);
        TextView tvRole = view.findViewById(R.id.tvDialogRole);

        tvAvatar.setText(employee.getAvatarText());
        tvName.setText(employee.getName());
        tvRole.setText(employee.getRole());

        dialog.setContentView(view);
        dialog.show();
    }

    public void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_sort, null);

        ChipGroup cgRole = view.findViewById(R.id.cgRole);
        ChipGroup cgDepartment = view.findViewById(R.id.cgDepartment);
        ChipGroup cgStatus = view.findViewById(R.id.cgStatus);

        // Lấy danh sách Duy nhất (Unique) từ allEmployees
        Set<String> roles = new HashSet<>();
        Set<String> depts = new HashSet<>();
        Set<String> statuses = new HashSet<>();

        for (Employee e : allEmployees) {
            roles.add(e.getRole());
            depts.add(e.getDepartment());
            statuses.add(e.getStatus());
        }

        // Hàm tạo Chip động
        addChipsToGroup(cgRole, roles);
        addChipsToGroup(cgDepartment, depts);
        addChipsToGroup(cgStatus, statuses);

        view.findViewById(R.id.btnApply).setOnClickListener(v -> {
            // Thu thập các Chip được chọn và thực hiện lọc
            List<String> selectedRoles = getSelectedChips(cgRole);
            List<String> selectedDepts = getSelectedChips(cgDepartment);
            // ... thực hiện hàm filter tương ứng
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void addChipsToGroup(ChipGroup group, Set<String> items) {
        group.removeAllViews();
        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Loại bỏ icon đóng (X) mặc định nếu không cần
            chip.setCloseIconVisible(false);

            // Áp dụng các file màu state list đã tạo
            chip.setChipBackgroundColorResource(R.color.chip_background_state);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_state, getTheme()));

            // Bỏ đường viền mặc định để giống ảnh mẫu hơn
            chip.setChipStrokeWidth(0f);

            group.addView(chip);
        }
    }
}