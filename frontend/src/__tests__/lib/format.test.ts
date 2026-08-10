import {describe, expect, it} from 'vitest';
import {formatBytes, formatCount, formatDuration} from '../../lib/format';

describe('format: heap analysis values', () => {
  it('formatBytes_1536Bytes_returnsOnePointFiveKilobytes', () => {
    const formatted = formatBytes(1_536);

    expect(formatted).toBe('1.5 KB');
  });

  it('formatCount_1234567_returnsGroupedDigits', () => {
    const formatted = formatCount(1_234_567);

    expect(formatted).toBe('1,234,567');
  });

  it('formatBytes_negative2048Bytes_preservesReductionSign', () => {
    const formatted = formatBytes(-2_048);

    expect(formatted).toBe('-2.0 KB');
  });

  it('formatDuration_1250Milliseconds_returnsOnePointThreeSeconds', () => {
    const formatted = formatDuration(1_250);

    expect(formatted).toBe('1.3 s');
  });

  it('formatDuration_koreanLocale_returnsKoreanSeconds', () => {
    const formatted = formatDuration(1_250, 'ko');

    expect(formatted).toBe('1.3초');
  });
});
