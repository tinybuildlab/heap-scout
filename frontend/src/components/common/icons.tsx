interface IconProps {
  readonly className?: string;
}

export function FolderIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M3.75 6.75A2.25 2.25 0 0 1 6 4.5h3.1l2 2.25H18A2.25 2.25 0 0 1 20.25 9v7.5A2.25 2.25 0 0 1 18 18.75H6a2.25 2.25 0 0 1-2.25-2.25V6.75Z" />
      <path d="M3.75 9h16.5" />
    </svg>
  );
}

export function ShieldIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 3.25 19 6v5.35c0 4.5-2.8 7.85-7 9.4-4.2-1.55-7-4.9-7-9.4V6l7-2.75Z" />
      <path d="m8.75 12 2.1 2.1 4.4-4.4" />
    </svg>
  );
}

export function StreamIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M4 7h10M4 12h16M4 17h12" />
      <path d="m16 4 3 3-3 3" />
    </svg>
  );
}

export function CompareIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M7 4v16M17 4v16M4 7l3-3 3 3M14 17l3 3 3-3" />
    </svg>
  );
}

export function SearchIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="10.75" cy="10.75" r="6.25" />
      <path d="m15.5 15.5 4 4" />
    </svg>
  );
}

export function SunIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="12" cy="12" r="3.75" />
      <path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M18.7 5.3l-1.4 1.4M6.7 17.3l-1.4 1.4" />
    </svg>
  );
}

export function MoonIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M20 15.2A8.5 8.5 0 0 1 8.8 4a8.5 8.5 0 1 0 11.2 11.2Z" />
    </svg>
  );
}

export function ArrowRightIcon({className}: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M5 12h14M14 7l5 5-5 5" />
    </svg>
  );
}
