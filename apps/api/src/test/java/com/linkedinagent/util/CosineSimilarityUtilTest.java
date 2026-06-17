package com.linkedinagent.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CosineSimilarityUtilTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        assertThat(CosineSimilarityUtil.similarity(vector, vector)).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        assertThat(CosineSimilarityUtil.similarity(a, b)).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void rejectsMismatchedDimensions() {
        float[] a = {1.0f, 2.0f};
        float[] b = {1.0f};
        assertThatThrownBy(() -> CosineSimilarityUtil.similarity(a, b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullOrEmptyVectorsReturnZero() {
        assertThat(CosineSimilarityUtil.similarity(null, new float[]{1.0f})).isZero();
        assertThat(CosineSimilarityUtil.similarity(new float[]{}, new float[]{})).isZero();
    }
}
