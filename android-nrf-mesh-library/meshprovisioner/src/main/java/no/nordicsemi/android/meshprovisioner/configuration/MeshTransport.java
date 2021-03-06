package no.nordicsemi.android.meshprovisioner.configuration;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import no.nordicsemi.android.meshprovisioner.messages.AccessMessage;
import no.nordicsemi.android.meshprovisioner.messages.ControlMessage;
import no.nordicsemi.android.meshprovisioner.messages.Message;
import no.nordicsemi.android.meshprovisioner.transport.LowerTransportLayerCallbacks;
import no.nordicsemi.android.meshprovisioner.transport.NetworkLayer;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

final class MeshTransport extends NetworkLayer {

    private static final String TAG = MeshTransport.class.getSimpleName();

    MeshTransport(final Context context, final ProvisionedMeshNode unprovisionedMeshNode) {
        super();
        this.mContext = context;
        this.mMeshNode = unprovisionedMeshNode;
        initHandler();
    }

    @Override
    protected void initHandler() {
        this.mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void setCallbacks(final LowerTransportLayerCallbacks callbacks) {
        super.setCallbacks(callbacks);
    }

    @Override
    protected int incrementSequenceNumber() {
        return SequenceNumber.incrementAndStore(mContext);
    }

    @Override
    protected int incrementSequenceNumber(final byte[] sequenceNumber) {
        return SequenceNumber.incrementAndStore(mContext, sequenceNumber);
    }

    /**
     * Creates the an acknowledgement message for the received segmented messages
     *
     * @param controlMessage Control message containing the required opcodes and parameters to create the message
     * @return Control message containing the acknowledgement message pdu
     */
    public ControlMessage createSegmentBlockAcknowledgementMessage(final ControlMessage controlMessage) {
        createLowerTransportControlPDU(controlMessage);
        createNetworkLayerPDU(controlMessage);
        return controlMessage;
    }

    /**
     * Creates an access message to be sent to the peripheral node
     * <p>
     * This method will create the access message and propagate the message through the transport layers to create the final mesh pdu
     * </p>
     *
     * @param unprovisionedMeshNode                ProvisionedMeshNode that is
     * @param src                     Source address of the provisioner/configurator.
     * @param key                     Key could be application key or device key.
     * @param akf                     Application key flag defines which key to be used to decrypt the message i.e device key or application key.
     * @param aid                     Identifier of the application key.
     * @param aszmic                  Defines the length of the transport mic length where 1 will encrypt withn 64 bit and 0 with 32 bit encryption.
     * @param accessOpCode            Operation code for the access message.
     * @param accessMessageParameters Parameters for the access message.
     * @return access message containing the mesh pdu
     */
    AccessMessage createMeshMessage(final ProvisionedMeshNode unprovisionedMeshNode, final byte[] src,
                                    final byte[] key, final int akf, final int aid, final int aszmic,
                                    final int accessOpCode, final byte[] accessMessageParameters) {

        final int sequenceNumber = incrementSequenceNumber();
        final byte[] sequenceNum = MeshParserUtils.getSequenceNumberBytes(sequenceNumber);

        final AccessMessage message = new AccessMessage();
        message.setSrc(src);
        message.setDst(unprovisionedMeshNode.getUnicastAddress());
        message.setIvIndex(unprovisionedMeshNode.getIvIndex());
        message.setSequenceNumber(sequenceNum);
        message.setKey(key);
        message.setAkf(akf);
        message.setAid(aid);
        message.setAszmic(aszmic);
        message.setOpCode(accessOpCode);
        message.setParameters(accessMessageParameters);
        message.setPduType(NETWORK_PDU);

        super.createMeshMessage(message);
        return message;
    }

    /**
     * Parses the received pdu
     *
     * @param configurationSrc Src address where the original message was sent from
     * @param pdu              pdu received
     * @return Message
     */
    public Message parsePdu(final byte[] configurationSrc, final byte[] pdu) {
        return parseMeshMessage(configurationSrc, pdu);
    }

    /**
     * Parses the received pdu
     *
     * @param pdu pdu received
     * @return Message
     */
    @VisibleForTesting
    public Message parsePdu(final byte[] pdu) {
        return parseMeshMessage(pdu);
    }

}
