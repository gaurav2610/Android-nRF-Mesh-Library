package no.nordicsemi.android.meshprovisioner.models;

import android.os.Parcel;

public class GenericManufacturerPropertyServer extends SigModel {

    public static final Creator<GenericManufacturerPropertyServer> CREATOR = new Creator<GenericManufacturerPropertyServer>() {
        @Override
        public GenericManufacturerPropertyServer createFromParcel(final Parcel source) {
            return new GenericManufacturerPropertyServer((short) source.readInt());
        }

        @Override
        public GenericManufacturerPropertyServer[] newArray(final int size) {
            return new GenericManufacturerPropertyServer[size];
        }
    };

    public GenericManufacturerPropertyServer(final int modelId) {
        super(modelId);
    }

    @Override
    public String getModelName() {
        return "Generic Manufacturer Property Server";
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
