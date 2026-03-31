package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminManageProfilesFragment extends Fragment {

    private final List<Users> allUsers = new ArrayList<>();
    private final List<Users> filteredUsers = new ArrayList<>();
    private AdminUserAdapter adapter;

    public AdminManageProfilesFragment() {
        super(R.layout.fragment_admin_manage_profiles);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBackToUser).setOnClickListener(v -> requireActivity().finish());

        RecyclerView rv = view.findViewById(R.id.rvAdminUsers);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new AdminUserAdapter(filteredUsers, user -> showUserDetail(user));
        rv.setAdapter(adapter);

        setupSearch(view);
        loadUsers();
    }

    private void setupSearch(View view) {
        EditText etSearch = view.findViewById(R.id.etSearchUsers);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase();
                filteredUsers.clear();
                if (query.isEmpty()) {
                    filteredUsers.addAll(allUsers);
                } else {
                    for (Users u : allUsers) {
                        String name = u.getName();
                        if (name == null || name.isEmpty()) {
                            String first = u.getFirstName() != null ? u.getFirstName() : "";
                            String last = u.getLastName() != null ? u.getLastName() : "";
                            name = (first + " " + last).trim();
                        }
                        if (name.toLowerCase().contains(query)
                                || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query))) {
                            filteredUsers.add(u);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Users user = doc.toObject(Users.class);
                        if (user.getId() == null) user.setId(doc.getId());
                        allUsers.add(user);
                    }
                    filteredUsers.clear();
                    filteredUsers.addAll(allUsers);
                    adapter.notifyDataSetChanged();
                });
    }

    private void showUserDetail(Users user) {
        AdminUserDetailBottomSheet sheet = AdminUserDetailBottomSheet.newInstance(user);
        sheet.setOnEventClickListener(event -> navigateToEventDetail(event));
        sheet.setOnAccountDeletedListener(userId -> {
            allUsers.removeIf(u -> userId.equals(u.getId()));
            filteredUsers.removeIf(u -> userId.equals(u.getId()));
            adapter.notifyDataSetChanged();
        });
        sheet.show(getChildFragmentManager(), "UserDetail");
    }

    private void navigateToEventDetail(Event event) {
        Bundle args = new Bundle();
        args.putSerializable("event", event);
        Navigation.findNavController(requireView())
                .navigate(R.id.adminEventDetailFragment, args);
    }
}
