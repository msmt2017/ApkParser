package com.arsc.data;

import com.arsc.stream.ArscStreamer;
import com.common.PrintUtil;

/**
 *
 * Created by xueqiulxq on 26/07/2017.
 */

public class ResTableTypeInfoChunk extends BaseTypeChunk {

    public static final long NO_ENTRY = 0xffffffffL;

    public ChunkHeader header;
    public int id;      // 1byte    resource type 0x00ff0000
    public int res0;    // 1byte
    public int res1;    // 2byte
    public long entryCount;
    public long entriesStart;       // start of table entries.
    public ResTableConfig resConfig;

    // Data Block
    public long[] entryOffsets;     // offset of table entries.
    public ResTableEntry[] tableEntries;
    public ResValue[] resValues;    // direct value;

    public static ResTableTypeInfoChunk parseFrom(ArscStreamer s, ResStringPoolChunk stringChunk) {
        ResTableTypeInfoChunk chunk = new ResTableTypeInfoChunk();
        int start = s.getCursor();
        chunk.header = ChunkHeader.parseFrom(s);
        chunk.id = s.readUInt8();
        chunk.res0 = s.readUInt8();
        chunk.res1 = s.readUShort();
        chunk.entryCount = s.readUInt();
        chunk.entriesStart = s.readUInt();
        chunk.resConfig = ResTableConfig.parseFrom(s);

        chunk.entryOffsets = new long[(int) chunk.entryCount];
        for (int i=0; i<chunk.entryCount; ++i) {
            chunk.entryOffsets[i] = s.readUInt();
        }

        chunk.tableEntries = new ResTableEntry[(int) chunk.entryCount];
        chunk.resValues = new ResValue[(int) chunk.entryCount];
        s.seek(start + chunk.entriesStart); // Locate entry start point.
        for (int i=0; i<chunk.entryCount; ++i) {
            // This is important!
            if (chunk.entryOffsets[i] == NO_ENTRY) {
                continue;
            }

            int cursor = s.getCursor();     // Remember the start cursor
            ResTableEntry entry = ResTableEntry.parseFrom(s);

            // We need to parse entry into ResTableMapEntry instead of ResTableEntry
            if (entry.flags == ResTableEntry.FLAG_COMPLEX) {
                s.seek(cursor);             // Rest cursor
                entry = ResTableMapEntry.parseFrom(s);      // Complex ResTableMapEntry
            } else {
                chunk.resValues[i] = ResValue.parseFrom(s); // ResTableEntry follows a ResValue
            }
            chunk.tableEntries[i] = entry;
        }

        return chunk;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        String form = "%-16s %s\n";
        String form3 = "%-16s %s \t%s\n";

        builder.append("-- ResTableTypeInfoChunk --\n");
        builder.append(header);
        builder.append(String.format(form3, "id", PrintUtil.hex1(id), "/* Reference into ResTablePackage::typeStringPool */"));
        builder.append(String.format(form, "res0", PrintUtil.hex1(res0)));
        builder.append(String.format(form, "res1", PrintUtil.hex1(res1)));
        builder.append(String.format(form, "entryCount", PrintUtil.hex4(entryCount)));
        builder.append(String.format(form, "entriesStart", PrintUtil.hex4(entriesStart)));
        builder.append(resConfig);

        builder.append(String.format("EntryOffset array: length=%d\n", entryOffsets.length));
        for (int i=0; i<entryOffsets.length; ++i) {
            builder.append(PrintUtil.hex4(entryOffsets[i])).append(' ');
            if ((i + 1) % 16 == 0) {
                builder.append('\n');
            }
        }
        if (entryOffsets.length % 16 != 0) {
            builder.append('\n');
        }
        builder.append('\n');

        builder.append(String.format("ResTableEntry array: length=%d\n", tableEntries.length));
        for (int i=0; i<tableEntries.length; ++i) {
            ResTableEntry entry = tableEntries[i];
            if (entry instanceof ResTableMapEntry) {
                builder.append(String.format("ResTableMapEntry ID = %d\n", i));
            } else {
                builder.append(String.format("ResTableEntry ID = %d\n", i));
            }
            if (entry == null) {
                builder.append("null: no_entry\n");
            } else {
                builder.append(entry);
            }

            if (resValues[i] != null) {     // if (entry.flags != 0x01)
                builder.append(String.format("ResValue ID = %d", i));
                builder.append(resValues[i]);
            }
            builder.append('\n');
        }

        return builder.toString();
    }

    @Override
    public String buildEntry2String(ResStringPoolChunk typeStringPool, ResStringPoolChunk keyStringPool) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<tableEntries.length; ++i) {
            String type = typeStringPool.getString(id-1);   // from 1
            if (tableEntries[i] != null) {
                builder.append(tableEntries[i].buildEntry2String(type, keyStringPool));
            } else {
                System.out.println("NO_ENTRY for " + type + " " + i);
            }
        }
        return builder.toString();
    }

    @Override
    public String getChunkName() {
        return "ResTableTypeInfoChunk";
    }
}
