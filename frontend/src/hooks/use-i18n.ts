import {useCallback} from 'react';
import {translate, type TranslationKey} from '../i18n/translations';
import {useUiStore} from '../stores/use-ui-store';

interface I18n {
  readonly locale: 'ko' | 'en';
  readonly t: (key: TranslationKey, parameters?: Readonly<Record<string, string | number>>) => string;
}

export function useI18n(): I18n {
  const locale = useUiStore((state) => state.locale);
  const t = useCallback(
    (key: TranslationKey, parameters?: Readonly<Record<string, string | number>>) => (
      translate(locale, key, parameters)
    ),
    [locale],
  );
  return {locale, t};
}
