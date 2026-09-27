import VideoGenerator from '../components/video/VideoGenerator'

export default function Generate() {
  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Generate a video</h1>
          <p className="page-subtitle">Describe a scene and choose a duration - we handle the rest.</p>
        </div>
      </div>
      <div style={{ maxWidth: 560 }}>
        <VideoGenerator />
      </div>
    </div>
  )
}
