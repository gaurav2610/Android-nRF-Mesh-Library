/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfmeshprovisioner;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.configuration.ConfigModelAppStatus;
import no.nordicsemi.android.meshprovisioner.configuration.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.configuration.MeshModel;
import no.nordicsemi.android.meshprovisioner.utils.Element;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.nrfmeshprovisioner.adapter.AddressAdapter;
import no.nordicsemi.android.nrfmeshprovisioner.di.Injectable;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentConfigurationStatus;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPublishAddress;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentSubscriptionAddress;
import no.nordicsemi.android.nrfmeshprovisioner.utils.Utils;
import no.nordicsemi.android.nrfmeshprovisioner.viewmodels.ModelConfigurationViewModel;
import no.nordicsemi.android.nrfmeshprovisioner.widgets.ItemTouchHelperAdapter;
import no.nordicsemi.android.nrfmeshprovisioner.widgets.RemovableItemTouchHelperCallback;
import no.nordicsemi.android.nrfmeshprovisioner.widgets.RemovableViewHolder;

import static no.nordicsemi.android.nrfmeshprovisioner.utils.Utils.EXTRA_DATA_MODEL_NAME;
import static no.nordicsemi.android.nrfmeshprovisioner.utils.Utils.EXTRA_DEVICE;
import static no.nordicsemi.android.nrfmeshprovisioner.utils.Utils.EXTRA_ELEMENT_ADDRESS;
import static no.nordicsemi.android.nrfmeshprovisioner.utils.Utils.EXTRA_MODEL_ID;

public class ModelConfigurationActivity extends AppCompatActivity implements Injectable,
        DialogFragmentConfigurationStatus.DialogFragmentAppKeyBindStatusListener,
        DialogFragmentPublishAddress.DialogFragmentPublishAddressListener,
        DialogFragmentSubscriptionAddress.DialogFragmentSubscriptionAddressListener, AddressAdapter.OnItemClickListener, ItemTouchHelperAdapter {

    private static final String DIALOG_FRAGMENT_CONFIGURATION_STATUS = "DIALOG_FRAGMENT_CONFIGURATION_STATUS";

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    @BindView(R.id.app_key_index)
    TextView mAppKeyView;
    @BindView(R.id.publish_address)
    TextView mPublishAddressView;
    @BindView(R.id.subscribe_addresses)
    TextView mSubscribeAddressView;
    @BindView(R.id.subscribe_hint)
    TextView mSubscribeHint;
    @BindView(R.id.configuration_progress_bar)
    ProgressBar mProgressbar;

    private Handler mHandler;
    private  ModelConfigurationViewModel mViewModel;
    private List<byte[]> mGroupAddress = new ArrayList<>();
    private AddressAdapter mAddressAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_configuration);
        ButterKnife.bind(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(ModelConfigurationViewModel.class);
        mHandler = new Handler();
        final Intent intent = getIntent();
        final ProvisionedMeshNode meshNode = intent.getParcelableExtra(EXTRA_DEVICE);
        final int elementAddress = intent.getExtras().getInt(EXTRA_ELEMENT_ADDRESS);
        final int modelId = intent.getExtras().getInt(EXTRA_MODEL_ID);
        if(meshNode == null)
            finish();

        final String modelName = intent.getStringExtra(EXTRA_DATA_MODEL_NAME);
        mViewModel.setModel(meshNode, elementAddress, modelId);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(modelName);
        final RecyclerView recyclerViewAddresses = findViewById(R.id.recycler_view_addresses);
        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        final ItemTouchHelper.Callback itemTouchHelperCallback = new RemovableItemTouchHelperCallback(this);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewAddresses);
        mAddressAdapter = new AddressAdapter(this, mViewModel.getMeshModel());
        recyclerViewAddresses.setAdapter(mAddressAdapter);
        mAddressAdapter.setOnItemClickListener(this);

        // Set up views
        mAppKeyView.setText(R.string.none);

        findViewById(R.id.action_bind_app_key).setOnClickListener(v -> {
            final Intent bindAppKeysIntent = new Intent(ModelConfigurationActivity.this, BindAppKeysActivity.class);
            bindAppKeysIntent.putExtra(ManageAppKeysActivity.APP_KEYS, (Serializable) mViewModel.getExtendedMeshNode().getMeshNode().getAddedAppKeys());
            startActivityForResult(bindAppKeysIntent, ManageAppKeysActivity.SELECT_APP_KEY);
        });

        mPublishAddressView.setText(R.string.none);
        findViewById(R.id.action_publish_Address).setOnClickListener(v -> {
            final DialogFragmentPublishAddress fragmentPublishAddress = DialogFragmentPublishAddress.newInstance();
            fragmentPublishAddress.show(getSupportFragmentManager(), null);
        });

        findViewById(R.id.action_subscribe_address).setOnClickListener(v -> {
            final DialogFragmentSubscriptionAddress fragmentSubscriptionAddress = DialogFragmentSubscriptionAddress.newInstance();
            fragmentSubscriptionAddress.show(getSupportFragmentManager(), null);
        });

        mViewModel.getMeshModel().observe(this, meshModel -> {
            if(meshModel != null) {
                final List<Integer> appKeys = meshModel.getBoundAppKeyIndexes();
                if (!appKeys.isEmpty()) {
                    mAppKeyView.setText(getString(R.string.app_keys_bound, appKeys.size()));
                } else {
                    mAppKeyView.setText(getString(R.string.no_app_keys_bound));
                }

                final byte[] publishAddress = meshModel.getPublishAddress();
                if (publishAddress != null) {
                    mPublishAddressView.setText(MeshParserUtils.bytesToHex(publishAddress, true));
                }

                final List<byte[]> subscriptionAddresses = meshModel.getSubscriptionAddresses();
                mGroupAddress.clear();
                mGroupAddress.addAll(subscriptionAddresses);
                if (!subscriptionAddresses.isEmpty()) {
                    mSubscribeHint.setVisibility(View.VISIBLE);
                    mSubscribeAddressView.setVisibility(View.GONE);
                    recyclerViewAddresses.setVisibility(View.VISIBLE);
                } else {
                    mSubscribeHint.setVisibility(View.GONE);
                    mSubscribeAddressView.setVisibility(View.VISIBLE);
                    recyclerViewAddresses.setVisibility(View.GONE);
                }
            }
        });

        mViewModel.getAppKeyBindStatusLiveData().observe(this, appKeyBindStatusLiveData -> {
            if(!appKeyBindStatusLiveData.isSuccess()){
                final String statusMessage = ConfigModelAppStatus.parseStatusMessage(this, appKeyBindStatusLiveData.getStatus());
                DialogFragmentConfigurationStatus fragmentAppKeyBindStatus = DialogFragmentConfigurationStatus.newInstance(getString(R.string.title_appkey_status), statusMessage);
                fragmentAppKeyBindStatus.show(getSupportFragmentManager(), DIALOG_FRAGMENT_CONFIGURATION_STATUS);
            } else {
                hide();
                final int elementAdd = appKeyBindStatusLiveData.getElementAddress();
                final int modelIdentifier = appKeyBindStatusLiveData.getModelIdentifier();
                final Element element = mViewModel.getExtendedMeshNode().getMeshNode().getElements().get(elementAdd);
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                final int boundAppKeySize = model.getBoundAppKeyIndexes().size();
                mAppKeyView.setText(getString(R.string.app_key_count, boundAppKeySize));
            }
        });

        mViewModel.getConfigModelPublicationStatusLiveData().observe(this, configModelPublicationStatusLiveData -> {
            if(!configModelPublicationStatusLiveData.isSuccessful()){
                final String statusMessage = ConfigModelAppStatus.parseStatusMessage(this, configModelPublicationStatusLiveData.getStatus());
                DialogFragmentConfigurationStatus fragmentAppKeyBindStatus = DialogFragmentConfigurationStatus.newInstance(getString(R.string.title_publlish_address_status), statusMessage);
                fragmentAppKeyBindStatus.show(getSupportFragmentManager(), DIALOG_FRAGMENT_CONFIGURATION_STATUS);
            } else {
                hide();
                final int elementAdd = configModelPublicationStatusLiveData.getElementAddressInt();
                final int modelIdentifier = configModelPublicationStatusLiveData.getModelIdentifier();
                final Element element = mViewModel.getExtendedMeshNode().getMeshNode().getElements().get(elementAdd);
                final MeshModel model = element.getMeshModels().get(modelIdentifier);
                final byte[] publishAddress = model.getPublishAddress();
                mPublishAddressView.setText(MeshParserUtils.bytesToHex(publishAddress, true));
            }
        });

        mViewModel.getConfigModelSubscriptionStatusLiveData().observe(this, configModelSubscriptionStatus -> {
            if(!configModelSubscriptionStatus.isSuccessful()){
                final String statusMessage = ConfigModelAppStatus.parseStatusMessage(this, configModelSubscriptionStatus.getStatus());
                DialogFragmentConfigurationStatus fragmentAppKeyBindStatus = DialogFragmentConfigurationStatus.newInstance(getString(R.string.title_publlish_address_status), statusMessage);
                fragmentAppKeyBindStatus.show(getSupportFragmentManager(), DIALOG_FRAGMENT_CONFIGURATION_STATUS);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ManageAppKeysActivity.SELECT_APP_KEY){
            if(resultCode == RESULT_OK){
                final String appKey = data.getStringExtra(ManageAppKeysActivity.RESULT_APP_KEY);
                final int appKeyIndex = data.getIntExtra(ManageAppKeysActivity.RESULT_APP_KEY_INDEX, -1);
                if(appKey != null){
                    mViewModel.bindAppKey(appKeyIndex);
                    showProgressbar();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onAppKeyBindStatusConfirmed() {

    }

    @Override
    public void setPublishAddress(final byte[] publishAddress) {
        mViewModel.sendConfigModelPublishAddressSet(publishAddress);
        showProgressbar();
    }

    @Override
    public void setSubscriptionAddress(final byte[] subscriptionAddress) {
        mViewModel.sendConfigModelSubscriptionAdd(subscriptionAddress);
        showProgressbar();
    }

    private void showProgressbar(){
        mProgressbar.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mOperationTimeout, 2500);
    }

    private void hide(){
        mProgressbar.setVisibility(View.INVISIBLE);
        mHandler.removeCallbacks(mOperationTimeout);
    }

    private final Runnable mOperationTimeout = new Runnable() {
        @Override
        public void run() {
            mProgressbar.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    public void onItemDismiss(final RemovableViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        if(!mGroupAddress.isEmpty()) {
            final byte[] address = mGroupAddress.get(position);
            mViewModel.sendConfigModelSubscriptionDelete(address);
            showProgressbar();
        }
    }

    @Override
    public void onItemClick(final int position, final byte[] address) {

    }
}
