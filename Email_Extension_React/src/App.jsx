import { useState } from 'react';
import { Container, Typography, TextField, Button, FormControl, CircularProgress, Select, Box, InputLabel, MenuItem } from '@mui/material';
import axios from 'axios';
import './App.css';

function App() {
  const [emailContent, setEmailContent] = useState('');
  const [emailTone, setEmailTone] = useState('');
  const [generatedReply, setGeneratedReply] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleGenerateReply = async () => {
    setLoading(true);
    setError(''); // Reset error message on new request
    try {
      const response = await axios.post("http://localhost:8080/api/emails/generate", { emailContent, emailTone });

      // Handle the response based on its type and content
      if (response && response.data) {
        const reply = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
        setGeneratedReply(reply);
      } else {
        setError('No reply generated. Please try again.');
      }
    } catch (err) {
      setError('Failed to generate email reply. Please try again.');
      console.error(err);
    } finally {
      setLoading(false); // Stop loading spinner
    }
  };

  return (
    <Container>
      <Typography variant="h4" component="h1" gutterBottom>
        Email Reply Generator
      </Typography>

      <TextField
        fullWidth
        label="Enter the Email"
        id="input-email"
        value={emailContent}
        onChange={(e) => setEmailContent(e.target.value)}
        multiline
        rows={6}
        margin='normal'
      />

      <FormControl fullWidth sx={{ marginBottom: '10px' }}>
        <InputLabel>Tone (Optional)</InputLabel>
        <Select
          value={emailTone}
          label="Tone (Optional)"
          onChange={(e) => setEmailTone(e.target.value)}
        >
          <MenuItem value="formal">Formal</MenuItem>
          <MenuItem value="casual">Casual</MenuItem>
          <MenuItem value="friendly">Friendly</MenuItem>
          <MenuItem value="professional">Professional</MenuItem>
        </Select>
      </FormControl>

      <Button
        variant="outlined"
        color="primary"
        onClick={handleGenerateReply}
        disabled={!emailContent || loading}
        sx={{ width: '100%', marginBottom: '20px' }}
      >
        {loading ? <CircularProgress size={24} /> : 'Generate Reply'}
      </Button>

      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      {generatedReply && (
        <Box sx={{ mt: 3 }}>
          <TextField
            fullWidth
            label="Generated Reply"
            id="generated-reply"
            value={generatedReply}
            multiline
            rows={6}
            InputProps={{ readOnly: true }}
          />

          <Button
            variant="outlined"
            sx={{ mt: 2 }}
            onClick={() => navigator.clipboard.writeText(generatedReply)}
          >
            Copy to Clipboard
          </Button>
        </Box>
      )}
    </Container>
  );
}

export default App;
