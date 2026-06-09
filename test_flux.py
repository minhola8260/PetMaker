import os
import json
import time
import requests

# .env 수동 파싱 함수 (dotenv 라이브러리가 설치되지 않은 환경 대응)
def load_env_manually():
    env_path = ".env"
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    os.environ[key.strip()] = val.strip()

# 수동으로 환경변수 로드
load_env_manually()

# API 키 설정
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
REPLICATE_API_KEY = os.getenv("REPLICATE_API_KEY")
HF_API_KEY = os.getenv("HF_API_KEY")

# 테스트용 환경 데이터 (사용자 실시간 맥락 가정)
ENVIRONMENT_DATA = {
    "location": "서울 종로구 (Seoul Jongno-gu)",
    "weather": "비 (Rain)",
    "temperature": "15°C",
    "timezone": "Evening (저녁)"
}

def generate_pet_info(env_data):
    """Gemini API를 통해 환경 기반 펫의 텍스트 스펙을 정의합니다."""
    print("1. Gemini API를 통해 펫 정보 생성 중...")
    
    url = f"https://generativelanguage.googleapis.com/v1/models/gemini-3.5-flash:generateContent?key={GEMINI_API_KEY}"
    
    prompt = f"""
    당신은 나만의 펫 메이커 AI 캐릭터 디자이너입니다.
    다음 제공되는 실시간 환경 데이터를 분석하여, 이 환경에 어울리는 세상에 하나뿐인 독창적이고 귀여운 판타지 디지털 펫을 창작하세요.
    
    [환경 데이터]
    - 위치: {env_data['location']}
    - 날씨: {env_data['weather']}
    - 기온: {env_data['temperature']}
    - 시간대: {env_data['timezone']}
    
    [요구 사항]
    반드시 다음 JSON 스키마 형식으로만 응답을 반환하세요. 마크다운 백틱(```json) 없이 순수 JSON 문자열로만 응답하세요:
    {{
        "name": "펫의 한글 이름 (예: 솔라, 레인, 구름이 등 환경에 맞게 작명)",
        "description": "펫의 전체적인 외형 묘사 (이미지 생성 프롬프트로 쓸 수 있게 형태, 색상, 고유한 특징 등을 자세히 서술, 한글)",
        "english_visual_prompt": "Flux 이미지 생성용 영문 프롬프트 (귀엽고 완성도 높은 3D 게임 캐릭터 스타일, 3d cute fantasy character concept art, standalone creature, centered, full body, solid background, detailed texture, {env_data['weather']} themes)",
        "personality": "성격 묘사 (한글)",
        "traits": ["성격을 대변하는 3개 이하의 단어형 특징 태그 목록"]
    }}
    """
    
    headers = {"Content-Type": "application/json"}
    payload = {
        "contents": [
            {
                "parts": [{"text": prompt}]
            }
        ]
    }
    
    response = requests.post(url, headers=headers, json=payload)
    if response.status_code != 200:
        raise Exception(f"Gemini API 에러: {response.status_code} - {response.text}")
        
    result_json = response.json()
    response_text = result_json["candidates"][0]["content"]["parts"][0]["text"].strip()
    
    # 마크다운 블록 제거 처리
    if response_text.startswith("```json"):
        response_text = response_text.replace("```json", "").replace("```", "").strip()
    elif response_text.startswith("```"):
        response_text = response_text.replace("```", "").strip()
        
    pet_info = json.loads(response_text)
    return pet_info

def generate_flux_image(visual_prompt, output_filename="pet_test_result.png"):
    """Replicate API를 먼저 시도하고, 실패 시 Hugging Face 무료 API로 폴백하여 Flux.1-schnell 모델로 이미지를 생성합니다."""
    image_bytes = None
    exception_log = ""

    # 1. Replicate 우선 시도 (키가 존재할 때)
    if REPLICATE_API_KEY:
        try:
            print(f"2. Replicate Flux.1-schnell 모델을 사용하여 이미지 예측 시작...")
            url = "https://api.replicate.com/v1/models/black-forest-labs/flux-schnell/predictions"
            headers = {
                "Authorization": f"Bearer {REPLICATE_API_KEY}",
                "Content-Type": "application/json"
            }
            payload = {
                "input": {
                    "prompt": visual_prompt,
                    "go_fast": True,
                    "megapixels": "1",
                    "num_outputs": 1,
                    "aspect_ratio": "1:1",
                    "output_format": "webp"
                }
            }
            response = requests.post(url, headers=headers, json=payload)
            if response.status_code in (200, 201):
                prediction = response.json()
                prediction_id = prediction["id"]
                get_url = prediction["urls"]["get"]
                print(f"   - 예측 태스크가 생성되었습니다 (ID: {prediction_id}). 완료를 대기 중...")
                
                status = prediction["status"]
                output_url = None
                attempts = 0
                
                while status not in ("succeeded", "failed", "canceled") and attempts < 12:
                    time.sleep(1.5)
                    poll_response = requests.get(get_url, headers=headers)
                    if poll_response.status_code == 200:
                        poll_data = poll_response.json()
                        status = poll_data["status"]
                        print(f"   - 현재 상태: {status}")
                        if status == "succeeded":
                            output_url = poll_data["output"]
                            if isinstance(output_url, list):
                                output_url = output_url[0]
                            break
                    attempts += 1
                
                if status == "succeeded" and output_url:
                    print(f"3. Replicate 이미지 생성 완료! 다운로드 중: {output_url}")
                    image_bytes = requests.get(output_url).content
                    if output_url.endswith(".webp") and output_filename.endswith(".png"):
                        output_filename = output_filename.replace(".png", ".webp")
                else:
                    exception_log += f"Replicate 완료 실패 (상태: {status}). "
            else:
                exception_log += f"Replicate 요청 실패 ({response.status_code}: {response.text}). "
        except Exception as e:
            exception_log += f"Replicate 예외: {e}. "

    # 2. Replicate 실패 또는 키가 없는 경우 Hugging Face 무료 API로 폴백
    if image_bytes is None:
        if HF_API_KEY:
            try:
                print("2. Replicate가 비어있거나 실패하여 Hugging Face 무료 API(FLUX.1-schnell)로 우회합니다...")
                url = "https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell"
                headers = {
                    "Authorization": f"Bearer {HF_API_KEY}",
                    "Content-Type": "application/json"
                }
                payload = {
                    "inputs": visual_prompt
                }
                response = requests.post(url, headers=headers, json=payload)
                if response.status_code == 200:
                    image_bytes = response.content
                    print("3. Hugging Face 이미지 생성 완료!")
                else:
                    raise Exception(f"HF API 에러: {response.status_code} - {response.text}")
            except Exception as e:
                exception_log += f"HuggingFace 예외: {e}."
                raise Exception(f"AI 이미지 생성 실패. 모든 채널이 오류입니다. 로그: {exception_log}")
        else:
            raise Exception(f"AI 이미지 생성 실패. Replicate가 실패했으며 폴백할 Hugging Face 토큰이 없습니다. 로그: {exception_log}")

    # 3. 파일 저장
    with open(output_filename, "wb") as f:
        f.write(image_bytes)
        
    print(f"4. 이미지 저장 성공! 파일 경로: {output_filename}")

def main():
    if not GEMINI_API_KEY or GEMINI_API_KEY.startswith("YOUR_"):
        print("[오류] GEMINI_API_KEY가 설정되지 않았습니다.")
        return
    if not REPLICATE_API_KEY and not HF_API_KEY:
        print("[오류] REPLICATE_API_KEY 또는 HF_API_KEY 중 하나는 설정되어 있어야 합니다.")
        return
        
    try:
        pet_info = generate_pet_info(ENVIRONMENT_DATA)
        print("\n--- 생성된 펫 정보 ---")
        print(f"이름: {pet_info['name']}")
        print(f"외형 묘사: {pet_info['description']}")
        print(f"성격: {pet_info['personality']}")
        print(f"특성 태그: {pet_info['traits']}")
        print(f"영문 이미지 생성 프롬프트: {pet_info['english_visual_prompt']}")
        print("----------------------\n")
        
        generate_flux_image(pet_info['english_visual_prompt'])
        
    except Exception as e:
        print(f"\n[오류 발생] {e}")

if __name__ == "__main__":
    main()
