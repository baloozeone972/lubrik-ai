interface PersonalitySliderProps {
  label: string
  value: number
  onChange: (value: number) => void
  icon?: string
  min?: number
  max?: number
}

export const PersonalitySlider = ({ 
  label, 
  value, 
  onChange, 
  icon, 
  min = 0, 
  max = 10 
}: PersonalitySliderProps) => {
  return (
    <div className="space-y-2">
      <div className="flex justify-between items-center">
        <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
          {icon && <span>{icon}</span>}
          {label}
        </label>
        <span className="text-sm font-semibold text-primary-600">{value}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-500"
      />
      <div className="flex justify-between text-xs text-gray-500">
        <span>Faible</span>
        <span>Élevé</span>
      </div>
    </div>
  )
}
