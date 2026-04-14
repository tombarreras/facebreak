package com.thomasjbarrerasconsulting.faces;

import org.junit.Test;

import static org.junit.Assert.*;

public class FrameMetadataTest {

    @Test
    public void builder_createsMetadataWithCorrectValues() {
        FrameMetadata metadata = new FrameMetadata.Builder()
                .setWidth(1920)
                .setHeight(1080)
                .setRotation(90)
                .build();

        assertEquals(1920, metadata.getWidth());
        assertEquals(1080, metadata.getHeight());
        assertEquals(90, metadata.getRotation());
    }

    @Test
    public void builder_defaultsToZero() {
        FrameMetadata metadata = new FrameMetadata.Builder().build();

        assertEquals(0, metadata.getWidth());
        assertEquals(0, metadata.getHeight());
        assertEquals(0, metadata.getRotation());
    }

    @Test
    public void builder_settersAreChainable() {
        FrameMetadata.Builder builder = new FrameMetadata.Builder();
        FrameMetadata.Builder result = builder.setWidth(100).setHeight(200).setRotation(270);

        assertSame(builder, result);
    }
}
