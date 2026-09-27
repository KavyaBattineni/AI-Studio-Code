const MAX_LENGTH = 2000

export default function PromptBox({ value, onChange, error }) {
  return (
    <div className="form-group">
      <label className="label" htmlFor="prompt">
        Describe your video
      </label>
      <textarea
        id="prompt"
        className={`input ${error ? 'has-error' : ''}`}
        placeholder="A futuristic city at sunset, flying cars weaving between neon skyscrapers..."
        rows={5}
        maxLength={MAX_LENGTH}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <div className="flex-between">
        {error ? <span className="field-error">{error}</span> : <span className="field-hint">Be specific about scene, style, and mood.</span>}
        <span className="field-hint">
          {value.length}/{MAX_LENGTH}
        </span>
      </div>
    </div>
  )
}
