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
import android.widget.Toast;

import com.example.mckeown_hw06.databinding.CommentListItemBinding;
import com.example.mckeown_hw06.databinding.ForumListItemBinding;
import com.example.mckeown_hw06.databinding.FragmentViewForumBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class ViewForumFragment extends Fragment {
    private static final String TAG = "View Forum Fragment: ";
    ViewForumFragment.ViewForumFragmentListener mListener;
    FragmentViewForumBinding binding;
    private FirebaseAuth mAuth;

    private static final String ARG_PARAM_FORUM = "param1";
    private static final String ARG_PARAM2 = "param2";

    Forum forumObject;
    String forumId;
    String creatorId;
    String title;
    String creator;
    String description;
    Date dateTime;

    List<String> list = new ArrayList<>();

    ViewForumFragment.CommentsListAdapter commentsListAdapter;
    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;

    public ViewForumFragment() {
        // Required empty public constructor
    }

    public static ViewForumFragment newInstance(Forum forum) {
        ViewForumFragment fragment = new ViewForumFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM_FORUM, forum);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            forumObject = (Forum) getArguments().getSerializable(ARG_PARAM_FORUM);
            forumId = forumObject.getForumId();
            creatorId = forumObject.getCreatorId();
            title = forumObject.getTitle();
            creator = forumObject.getCreator();
            description = forumObject.getDescription();
            dateTime = forumObject.getDateTime();
        }

        getComments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentViewForumBinding.inflate(inflater, container, false);

        getComments();

        binding.buttonPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String commentText = binding.editTextComment.getText().toString();
                if (commentText.isEmpty()) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please enter a comment and then click post", Toast.LENGTH_SHORT).show();
                } else {
                    createComment();
                    binding.editTextComment.setText("");
                }
            }
        });

        recyclerView = binding.commentsRecyclerView;
        recyclerView.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        commentsListAdapter = new ViewForumFragment.CommentsListAdapter(list);
        recyclerView.setAdapter(commentsListAdapter);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textViewTitle.setText(title);
        binding.textViewForumCreator.setText(creator);
        binding.textViewDescription.setText(description);
    }

    private void createComment() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String name = user.getDisplayName();
        String Uid = user.getUid();
        String comment = binding.editTextComment.getText().toString();

        HashMap<String, Object> data = new HashMap<>();

        data.put("commenterId", Uid);
        data.put("commenter", name);
        data.put("comment", comment);
        data.put("dateTime", Timestamp.now());

        db.collection("forums")
                .document(forumId)
                .collection("comments")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "New comment was successfully posted");
                        Log.d(TAG, "onSuccess: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error posting new comment" + e);
                    }
                });
    }

    private void getComments() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums")
                .document(forumId)
                .collection("comments")
                //.get()
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        list.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Log.d(TAG, "onComplete: comment =>" + document.getData());
                            list.add(document.getId());
                        }
                        Log.d(TAG, "list data: " + list);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.textViewNumComments.setText(list.size() + " Comments");
                                }
                            });
                        }
                    }
                });
    }

    class CommentsListAdapter extends RecyclerView.Adapter<ViewForumFragment.CommentsListAdapter.ViewCommentsViewHolder> {
        List<String> mComments;

        public CommentsListAdapter(List<String> data) {
            this.mComments = data;
        }

        @NonNull
        @Override
        public ViewForumFragment.CommentsListAdapter.ViewCommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CommentListItemBinding binding = CommentListItemBinding.inflate(getLayoutInflater(), parent, false);
            return new ViewForumFragment.CommentsListAdapter.ViewCommentsViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewForumFragment.CommentsListAdapter.ViewCommentsViewHolder holder, int position) {
            String comment = String.valueOf(mComments.get(position));
            holder.setupUI(comment);
        }

        @Override
        public int getItemCount() {
            return this.mComments.size();
        }

        public class ViewCommentsViewHolder extends RecyclerView.ViewHolder {
            CommentListItemBinding mBinding;
            String mComment;
            int position;


            public ViewCommentsViewHolder(@NonNull CommentListItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            public void setupUI(String comment) {
                mComment = comment;

                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                String userId = user.getUid();

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("forums")
                        .document(forumId)
                        .collection("comments")
                        .document(mComment)
                        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                    mAuth = FirebaseAuth.getInstance();
                                    String name = value.getString("commenter");
                                    String text = value.getString("comment");
                                    String commenterId = value.getString("commenterId");
                                    Date dateTime = value.getDate("dateTime");
                                    Log.d(TAG, "Uid = " + userId + " => commenterId = " + commenterId);

                                    mBinding.textViewCommentCreator.setText(name);
                                    mBinding.textViewComment.setText(text);

                                    if (dateTime != null) {
                                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mma");
                                        String formattedDate = sdf.format(dateTime);
                                        mBinding.textViewCommentDateTime.setText(formattedDate);
                                    }

                                    if (commenterId != null) {
                                        if (userId.matches(commenterId)) {
                                            mBinding.imageFilterViewTrashCan.setImageResource(R.drawable.rubbish_bin);
                                            mBinding.imageFilterViewTrashCan.setVisibility(View.VISIBLE);
                                            mBinding.imageFilterViewTrashCan.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    deleteComment(mComment);
                                                    getComments();
                                                }
                                            });
                                        } else {
                                            mBinding.imageFilterViewTrashCan.setVisibility(View.INVISIBLE);
                                        }
                                    }
                            }
                        });
            }
        }
    }

    private void deleteComment(String id) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("forums")
                .document(forumId)
                .collection("comments")
                .document(id)
                .delete();
        Log.d(TAG, "deleteComment: Delete successful");
        getComments();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (ViewForumFragment.ViewForumFragmentListener) context;
    }

    interface ViewForumFragmentListener {

    }
}