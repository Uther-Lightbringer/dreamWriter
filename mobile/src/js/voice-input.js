export class VoiceInput {
  constructor() {
    this.recognition = null;
    this.isListening = false;
    this.onResult = null;
    this.onError = null;
  }

  init() {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      console.warn('Web Speech API not supported');
      return false;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    this.recognition = new SpeechRecognition();
    this.recognition.lang = 'zh-CN';
    this.recognition.continuous = true;
    this.recognition.interimResults = true;

    this.recognition.onresult = (event) => {
      let finalTranscript = '';
      let interimTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript;
        } else {
          interimTranscript += transcript;
        }
      }

      if (this.onResult) {
        this.onResult(finalTranscript, interimTranscript);
      }
    };

    this.recognition.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
      this.isListening = false;
      if (this.onError) {
        this.onError(event.error);
      }
    };

    this.recognition.onend = () => {
      this.isListening = false;
    };

    return true;
  }

  start() {
    if (!this.recognition) {
      const supported = this.init();
      if (!supported) return false;
    }

    try {
      this.recognition.start();
      this.isListening = true;
      return true;
    } catch (error) {
      console.error('Failed to start recognition:', error);
      return false;
    }
  }

  stop() {
    if (this.recognition && this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    }
  }

  toggle() {
    if (this.isListening) {
      this.stop();
      return false;
    } else {
      return this.start();
    }
  }
}

export function attachVoiceButton(inputElement, buttonElement) {
  const voiceInput = new VoiceInput();
  
  buttonElement.addEventListener('click', () => {
    const isListening = voiceInput.toggle();
    
    if (isListening) {
      buttonElement.classList.add('text-red-500', 'animate-pulse');
      voiceInput.onResult = (final, interim) => {
        if (final) {
          inputElement.value += final;
        }
      };
      voiceInput.onError = (error) => {
        console.error('Voice input error:', error);
        buttonElement.classList.remove('text-red-500', 'animate-pulse');
      };
    } else {
      buttonElement.classList.remove('text-red-500', 'animate-pulse');
    }
  });

  return voiceInput;
}
