package com.example.mckeown_hw06;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mckeown_hw06.databinding.ForumListItemBinding;
import com.example.mckeown_hw06.databinding.FragmentCreateForumBinding;
import com.example.mckeown_hw06.databinding.FragmentForumsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ForumsFragment extends Fragment {
    private static final String TAG = "Forums List Fragment: ";
    ForumsFragment.ForumsFragmentListener mListener;
    FragmentForumsBinding binding;
    private FirebaseAuth mAuth;
    private ArrayList<Forum> forums = new ArrayList<>();
    ForumsListAdapter forumsListAdapter;
    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;

    List<String> favorites = new ArrayList<>();

    public ForumsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String name = user.getDisplayName();
        Log.d(TAG, "onCreate Forums Fragment: ");
        Log.d(TAG, "user ---------------- " + user);
        Log.d(TAG, "Logged In User Display Name ---------------- " + name);
        getData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForumsBinding.inflate(inflater, container, false);

        getData();

        binding.buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                mListener.goToLogin();
            }
        });

        binding.buttonNewForum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.goToCreateNewForum();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Forums");
    }

    private void getData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        forums.clear();
                        for (QueryDocumentSnapshot document: value) {
                            Forum forum = new Forum();
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            forum.setForumId(document.getId());
                            forum.setTitle(document.getString("title"));
                            forum.setForumId(document.getId());
                            forum.setCreator(document.getString("creator"));
                            forum.setCreatorId(document.getString("creatorId"));
                            forum.setDescription(document.getString("description"));
                            forum.setDateTime(document.getDate("dateTime"));
                            forums.add(forum);
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    recyclerView = binding.forumsRecyclerView;
                                    recyclerView.setHasFixedSize(true);
                                    linearLayoutManager = new LinearLayoutManager(getContext());
                                    recyclerView.setLayoutManager(linearLayoutManager);
                                    forumsListAdapter = new ForumsListAdapter(forums);
                                    recyclerView.setAdapter(forumsListAdapter);
                                }
                            });
                        }
                    }
                });
    }

    class ForumsListAdapter extends RecyclerView.Adapter<ForumsListAdapter.ForumsViewHolder> {
        ArrayList<Forum> mForums;

        public ForumsListAdapter(ArrayList<Forum> data) {
            this.mForums = data;
        }

        @NonNull
        @Override
        public ForumsListAdapter.ForumsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
           ForumListItemBinding binding = ForumListItemBinding.inflate(getLayoutInflater(), parent, false);
            return new ForumsViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ForumsListAdapter.ForumsViewHolder holder, int position) {
            Forum forum = mForums.get(position);
            holder.setupUI(forum);
        }

        @Override
        public int getItemCount() {
            return this.mForums.size();
        }

        public class ForumsViewHolder extends RecyclerView.ViewHolder {
            ForumListItemBinding mBinding;
            Forum mForum;
            int position;


            public ForumsViewHolder(@NonNull ForumListItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            public void setupUI(Forum forum) {
                mForum = forum;
                mBinding.textViewForumTitle.setText(mForum.title);
                mBinding.textViewCreator.setText(mForum.creator);
                mBinding.textViewForumDescription.setText(mForum.description);
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                String userId = user.getUid();
                Log.d(TAG, "user id: " + userId);
                Log.d(TAG, "creator id:  " + mForum.creatorId);
                SimpleDateFormat sdf = new SimpleDateFormat ("MM/dd/yyyy hh:mma");
                String formattedDate = sdf.format(mForum.dateTime);
                mBinding.textViewDateTime.setText(formattedDate);
                mBinding.textViewLikes.setText(" Likes | ");


                if (userId.matches(mForum.creatorId)) {
                    mBinding.imageFilterViewTrash.setImageResource(R.drawable.rubbish_bin);
                    mBinding.imageFilterViewTrash.setVisibility(View.VISIBLE);
                    mBinding.imageFilterViewTrash.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("forums")
                                    .document(mForum.forumId)
                                    .delete();
                            getData();
                        }
                    });
                } else {
                    mBinding.imageFilterViewTrash.setVisibility(View.INVISIBLE);
                }

                getFavoriteList(mForum.forumId);

                mBinding.imageViewHeart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mBinding.imageViewHeart.setImageResource(R.drawable.like_favorite);
                        getData();
                    }
                });

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Forum selectedForum = mForum;
                        Log.d(TAG, "onClick: Selected Forum Id " + selectedForum);
                        mListener.sendSelectedForum(selectedForum);
                    }
                });
            }
        }
    }

    private void getFavoriteList(String forumId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums")
                .document(forumId)
                .collection("favoriteList")
                //.get()
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        for (QueryDocumentSnapshot document : value) {
                            Log.d(TAG, "onComplete: favorite =>" + document.getData());
                            favorites.add(document.getId());
                        }
                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (ForumsFragment.ForumsFragmentListener) context;
    }

    interface ForumsFragmentListener {
        void goToLogin();
        void goToCreateNewForum();
        void sendSelectedForum(Forum forum);
    }
}