import {describe, expect, it} from 'vitest';
import {translate, translateError} from '../../i18n/translations';

describe('translations: Korean and English interface copy', () => {
  it('translate_koreanLocale_returnsKoreanLandingTitle', () => {
    const title = translate('ko', 'landing.title');

    expect(title).toContain('힙 덤프');
  });

  it('translate_fileNameParameter_interpolatesFileName', () => {
    const title = translate('ko', 'progress.readingTitle', {fileName: 'demo.hprof'});

    expect(title).toBe('demo.hprof 분석 중');
  });

  it('translateError_pickerUnavailable_returnsActionableKoreanMessage', () => {
    const message = translateError('ko', {
      code: 'FILE_PICKER_UNAVAILABLE',
      message: 'backend fallback',
    });

    expect(message).toContain('경로 입력란');
  });
});
