package no.nordicsemi.android.meshprovisioner.models;

import android.os.Parcel;

public class LightLightnessClient extends SigModel {

    public static final Creator<LightLightnessClient> CREATOR = new Creator<LightLightnessClient>() {
        @Override
        public LightLightnessClient createFromParcel(final Parcel source) {
            return new LightLightnessClient((short) source.readInt());
        }

        @Override
        public LightLightnessClient[] newArray(final int size) {
            return new LightLightnessClient[size];
        }
    };

    public LightLightnessClient(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Light Lightness Client";
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
