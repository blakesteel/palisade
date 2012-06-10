package com.jcraft.jorbis;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import java.io.*;

public class JOrbisAdapter {
    public static class VorbisFile {

        private static final int CHUNK_SIZE = 4096;
        private static final int OV_FALSE = -1;
        private static final int OV_EOF = -2;

        private final byte[] data;
        private int dataStartOffset;
        private int dataEndOffset;
        private int dataPosition;

        private int nextPageOffset = 0;

        private final Info info;
        private final Comment comment;
        private int serialno;
        private int numFrames;
        private int framePosition;
        private boolean decodeReady;

        private final SyncState oy = new SyncState();
        private final StreamState os = new StreamState();
        private final DspState vd = new DspState();
        private final Block vb = new Block(vd);

        private int[] _index;
        private float[][][] _pcm = new float[1][][];

        private VorbisFile(VorbisFile file) {
            this.data = file.data;
            this.info = file.info;
            this.comment = file.comment;
            this.serialno = file.serialno;
            this.numFrames = file.numFrames;
            this.framePosition = 0;
            this.decodeReady = false;
            slice(file.dataStartOffset, file.dataEndOffset);
            this._index = new int[info.channels];
        }
        
        public VorbisFile(byte[] data) throws JOrbisException {
            this(data, 0, data.length);
        }

        public VorbisFile(byte[] data, int offset, int length) throws JOrbisException {
            this.data = data;
            this.info = new Info();
            this.comment = new Comment();
            slice(offset, offset + length);
            open();
        }
        
        public VorbisFile(File file) throws JOrbisException {
            try {
                this.info = new Info();
                this.comment = new Comment();

                InputStream is = new FileInputStream(file);

                long length = file.length();

                if (length > Integer.MAX_VALUE) {
                    throw new JOrbisException("File too large to read.");
                }

                byte[] bytes = new byte[(int)length];
                int offset = 0;
                int numRead = 0;

                while (offset < bytes.length
                    && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                    offset += numRead;
                }

                // Ensure all the bytes have been read in
                if (offset < bytes.length) {
                    throw new JOrbisException("Could not completely read file "+file.getName());
                }

                // Close the input stream and return bytes
                is.close();

                this.data = bytes;
                slice(0, 0 + (int)length);
                open();
            }
            catch (Exception ex) {
                throw new JOrbisException(ex.getMessage());
            }
        }

        /**
            Create a copy of this VorbisFile. The returned file will share the same data. This is
            useful for playing multiple copies of the same file simultaneously.
        */
        public VorbisFile duplicate() {
            return new VorbisFile(this);
        }

        public boolean equals(Object obj) {
            return (obj instanceof VorbisFile) && data == ((VorbisFile)obj).data;
        }

        public void rewind() {
            this.framePosition = 0;
            this.decodeReady = false;
            this.dataPosition = dataStartOffset;
            this.nextPageOffset = dataPosition;
        }

        public boolean isRunning() {
            return decodeReady;
        }

        // Data buffer methods

        private int remaining() {
            return dataEndOffset - dataPosition;
        }

        private int get(byte[] dest, int offset, int length) {
            if (length > remaining()) {
                length = remaining();
            }
            System.arraycopy(data, dataPosition, dest, offset, length);
            dataPosition += length;
            return length;
        }

        private void slice(int startOffset, int endOffset) {
            dataStartOffset = startOffset;
            dataEndOffset = endOffset;
            dataPosition = startOffset;
            nextPageOffset = startOffset;
        }

        // Public methods (file info)

        public int getSampleRate() {
            return info.rate;
        }

        public int getNumChannels() {
            return info.channels;
        }

        public int getNumFrames() {
            return numFrames;
        }

        public Comment getComment() {
            return comment;
        }

        public float getDuration() {
            return getNumFrames() / (float)getSampleRate();
        }

        public int getFramePosition() {
            return framePosition;
        }

        // Private methods

        private void open() throws JOrbisException {
            getHeaders();
            os.clear();
            getEnd();
            this._index = new int[info.channels];
        }

        private void getHeaders() throws JOrbisException {
            Page og = new Page();
            Packet op = new Packet();
            if (getNextPage(og, CHUNK_SIZE) < 0) {
                throw new JOrbisException();
            }

            this.serialno = og.serialno();
            os.init(serialno);
            info.init();
            comment.init();

            int i = 0;
            while (i < 3) {
                os.pagein(og);
                while (i < 3) {
                    int result = os.packetout(op);
                    if (result == 0) {
                        break;
                    }
                    else if (result == -1) {
                        throw new JOrbisException();
                    }
                    else if (info.synthesis_headerin(comment, op) != 0) {
                        throw new JOrbisException();
                    }
                    i++;
                }
                if (i < 3 && getNextPage(og, 1) < 0) {
                    throw new JOrbisException();
                }
            }
        }

        private void getEnd() throws JOrbisException {
            Page og = new Page();
            int startOffset = nextPageOffset;
            int endOffset = dataEndOffset;
            numFrames = -1;

            while (true) {
                int ret = getNextPage(og, CHUNK_SIZE);
                if (ret == OV_EOF) {
                    endOffset = nextPageOffset;
                    break;
                }
                else if (ret < 0) {
                    throw new JOrbisException();
                }
                else if (serialno == og.serialno()) {
                    if (og.granulepos() != -1) {
                        numFrames = (int)og.granulepos();
                    }
                    if (og.eos() != 0) {
                        endOffset = nextPageOffset;
                        break;
                    }
                }
                else {
                    endOffset = ret;
                    break;
                }
            }

            if (numFrames < 0) {
                throw new JOrbisException();
            }

            oy.reset();

            slice(startOffset, endOffset);
        }

        /**
            On success, nextPageOffset is set.
            @return negative value on error; otherwise, the offset of the start of the page.
        */
        private int getNextPage(Page page, int boundary) {
            if (boundary > 0) {
                boundary += nextPageOffset;
            }
            while (true) {
                if (boundary > 0 && nextPageOffset >= boundary) {
                    return OV_FALSE;
                }
                int more = oy.pageseek(page);
                if (more < 0) {
                    nextPageOffset -= more;
                }
                else if (more == 0) {
                    if (boundary == 0) {
                        return OV_FALSE;
                    }
                    int index = oy.buffer(CHUNK_SIZE);
                    int bytes = get(oy.data, index, CHUNK_SIZE);
                    oy.wrote(bytes);
                    if (bytes == 0) {
                        return OV_EOF;
                    }
                }
                else {
                    int ret = nextPageOffset;
                    nextPageOffset += more;
                    return ret;
                }
            }
        }

        /**
            @return -1 for lost packet, 0 not enough data, or 1 for success
        */
        private int processPacket(boolean readPage) {
            Page og = new Page();

            while (true) {
                if (decodeReady) {
                    Packet op = new Packet();
                    int result = os.packetout(op);
                    if (result > 0) {
                        int granulepos = (int)op.granulepos;
                        if (vb.synthesis(op) == 0) {
                            vd.synthesis_blockin(vb);
                            if (granulepos != -1 && op.e_o_s == 0) {
                                int samples = vd.synthesis_pcmout(null, null);
                                granulepos -= samples;
                                framePosition = granulepos;
                            }
                            return(1);
                        }
                    }
                }

                if (!readPage || getNextPage(og, -1) < 0) {
                    return(0);
                }

                if (!decodeReady) {
                    os.init(serialno);
                    os.reset();
                    vd.synthesis_init(info);
                    vb.init(vd);
                    decodeReady = true;
                }
                os.pagein(og);
            }
        }

        // Public read methods

        public void setFramePosition(int newFramePosition) {
            if (newFramePosition < 0) {
                newFramePosition = 0;
            }
            else if (newFramePosition > numFrames) {
                newFramePosition = numFrames;
            }

            if (newFramePosition < framePosition) {
                rewind();
            }

            int framesToSkip = newFramePosition - framePosition;
            while (framesToSkip > 0) {
                int f = skip(framesToSkip);
                if (f < 0) {
                    return;
                }
                else {
                    framesToSkip -= f;
                }
            }
        }

        /**
            @return number of frames skipped, or -1 on error.
        */
        public int skip(int numFrames) {
            while (true) {
                if (decodeReady) {
                    int frames = vd.synthesis_pcmout(_pcm, _index);
                    if (frames != 0) {
                        int channels = info.channels;
                        if (frames > numFrames) {
                            frames = numFrames;
                        }
                        vd.synthesis_read(frames);
                        framePosition += frames;
                        return frames;
                    }
                }

                if (processPacket(true) <= 0) {
                    return -1;
                }
            }
        }

        /**
            @param dest destination buffer
            @param destOffset offset in the destination buffer
            @param destChannels number of channels in the destination (either 1 or 2).
            @param numFrames number of frames to read.
            @return number of frames read, or -1 on error. Always fails if this Vorbis file has more
            than two channels.
        */
        public int read(byte[] dest, int destOffset, int destChannels, int numFrames) {
            while (true) {
                if (decodeReady) {
                    int frames = vd.synthesis_pcmout(_pcm, _index);
                    if (frames != 0) {
                        int channels = info.channels;
                        if (frames > numFrames) {
                            frames = numFrames;
                        }

                        // Convert to signed, little endian, 16-bit PCM format.
                        if (destChannels == channels) {
                            // Mono-to-mono or stereo-to-stereo
                            int frameSize = 2 * channels;
                            for (int i = 0; i < channels; i++) {
                                int ptr = destOffset + 2 * i;
                                int mono = _index[i];
                                float[] pcm_row = _pcm[0][i];
                                for (int j = 0; j < frames; j++) {
                                    int sample = (int) (pcm_row[mono + j] * 32767);
                                    if (sample > 32767) {
                                        sample = 32767;
                                    }
                                    else if (sample < -32768) {
                                        sample = -32768;
                                    }

                                    dest[ptr] = (byte)sample;
                                    dest[ptr + 1] = (byte)(sample >> 8);
                                    ptr += frameSize;
                                }
                            }
                        }
                        else if (channels == 1 && destChannels == 2) {
                            // Mono-to-stereo
                            int ptr = destOffset;
                            int mono = _index[0];
                            float[] pcm_row = _pcm[0][0];
                            for (int j = 0; j < frames; j++) {
                                int sample = (int)(pcm_row[mono + j] * 32767);
                                if (sample > 32767) {
                                    sample = 32767;
                                }
                                else if (sample < -32768) {
                                    sample = -32768;
                                }

                                byte a = (byte)sample;
                                byte b = (byte)(sample >> 8);
                                dest[ptr++] = a;
                                dest[ptr++] = b;
                                dest[ptr++] = a;
                                dest[ptr++] = b;
                            }
                        }
                        else if (destChannels == 1) {
                            // Mix all channels to 1 (not tested)
                            for (int j = 0; j < frames * 2; j++) {
                                dest[destOffset + j] = 0;
                            }

                            for (int i = 0; i < channels; i++) {
                                int ptr = destOffset;
                                int mono = _index[i];
                                float[] pcm_row = _pcm[0][i];
                                for (int j = 0; j < frames; j++) {
                                    int oldSample = (dest[ptr] & 0xff) | (dest[ptr+1] << 8);
                                    int sample = (int)(oldSample +
                                        pcm_row[mono + j] * 32767 / channels);
                                    if (sample > 32767) {
                                        sample = 32767;
                                    }
                                    else if (sample < -32768) {
                                        sample = -32768;
                                    }

                                    dest[ptr++] = (byte)sample;
                                    dest[ptr++] = (byte)(sample >> 8);
                                }
                            }
                        }
                        else {
                            return -1;
                        }

                        vd.synthesis_read(frames);
                        framePosition += frames;
                        return frames;
                    }
                }

                if (processPacket(true) <= 0) {
                    return -1;
                }
            }
        }
    }
}