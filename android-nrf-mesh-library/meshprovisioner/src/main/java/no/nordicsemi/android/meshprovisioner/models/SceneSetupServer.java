package no.nordicsemi.android.meshprovisioner.models;

import android.os.Parcel;

public class SceneSetupServer extends SigModel {

    public static final Creator<SceneSetupServer> CREATOR = new Creator<SceneSetupServer>() {
        @Override
        public SceneSetupServer createFromParcel(final Parcel source) {
            return new SceneSetupServer((short) source.readInt());
        }

        @Override
        public SceneSetupServer[] newArray(final int size) {
            return new SceneSetupServer[size];
        }
    };

    public SceneSetupServer(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Scene Setup Server";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(mModelId);
    }
}
