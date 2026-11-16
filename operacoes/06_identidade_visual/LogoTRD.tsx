import React from 'react';

export interface LogoTRDProps extends React.SVGProps<SVGSVGElement> {
  variant?: 'horizontal' | 'stacked' | 'icon';
  theme?: 'light' | 'dark' | 'auto';
  className?: string;
  size?: number | string;
}

export const LogoTRD: React.FC<LogoTRDProps> = ({
  variant = 'horizontal',
  theme = 'light',
  className = '',
  size,
  ...props
}) => {
  const isDark = theme === 'dark';
  const primaryColor = isDark ? '#F8FAFC' : '#080D1A';
  const subtitleColor = isDark ? '#94A3B8' : '#1E293B';

  if (variant === 'icon') {
    return (
      <svg
        viewBox="0 0 160 160"
        width={size || 48}
        height={size || 48}
        className={className}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        {...props}
      >
        <g transform="translate(23, 20)">
          <polygon points="60,6 114,37 60,68 6,37" fill={primaryColor} />
          <polygon points="6,45 54,72 54,118 6,91" fill={primaryColor} />
          <polygon points="66,72 114,45 114,91 66,118" fill={primaryColor} />
        </g>
      </svg>
    );
  }

  if (variant === 'stacked') {
    return (
      <svg
        viewBox="0 0 400 400"
        width={size || 200}
        height={size || 200}
        className={className}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        {...props}
      >
        <g transform="translate(200, 70)">
          <g transform="translate(-70, 0)">
            <polygon points="70,7 133,43 70,79 7,43" fill={primaryColor} />
            <polygon points="7,52 63,84 63,138 7,106" fill={primaryColor} />
            <polygon points="77,84 133,52 133,106 77,138" fill={primaryColor} />
          </g>
        </g>
        <g textAnchor="middle">
          <text
            x="200"
            y="275"
            fontFamily="system-ui, -apple-system, sans-serif"
            fontSize="76"
            fontWeight="900"
            letterSpacing="4"
            fill={primaryColor}
          >
            TRD
          </text>
          <text
            x="200"
            y="320"
            fontFamily="system-ui, -apple-system, sans-serif"
            fontSize="16"
            fontWeight="600"
            letterSpacing="7"
            fill={subtitleColor}
          >
            CONSULTORIA DE TI
          </text>
        </g>
      </svg>
    );
  }

  // Padrão: Horizontal Lockup 1:1
  return (
    <svg
      viewBox="0 0 650 160"
      width={size || 320}
      className={className}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <g transform="translate(40, 20)">
        <g id="keystone-symbol">
          <polygon points="60,6 114,37 60,68 6,37" fill={primaryColor} />
          <polygon points="6,45 54,72 54,118 6,91" fill={primaryColor} />
          <polygon points="66,72 114,45 114,91 66,118" fill={primaryColor} />
        </g>
        <g id="typography" transform="translate(155, 0)">
          <text
            x="0"
            y="70"
            fontFamily="system-ui, -apple-system, sans-serif"
            fontSize="74"
            fontWeight="900"
            letterSpacing="4"
            fill={primaryColor}
          >
            TRD
          </text>
          <text
            x="2"
            y="112"
            fontFamily="system-ui, -apple-system, sans-serif"
            fontSize="16.5"
            fontWeight="600"
            letterSpacing="7.8"
            fill={subtitleColor}
          >
            CONSULTORIA DE TI
          </text>
        </g>
      </g>
    </svg>
  );
};

export default LogoTRD;
